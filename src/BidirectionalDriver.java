//import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;

public class BidirectionalDriver {
	private int source;
	private	int destination;
	private	double start_departure_time;
	private	double end_departure_time;
	private	double budget;
	
	public BidirectionalDriver(Query query, double budget) {
		this.source = query.get_source();
		this.destination = query.get_destination();
		this.start_departure_time = query.get_start_departure_time();
		this.end_departure_time = query.get_end_departure_time();
		this.budget = budget;
	}

	static class SharedState {
        ConcurrentHashMap<Integer, List<Label>> forwardVisited = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, List<Label>> backwardVisited = new ConcurrentHashMap<>();
        Set<Integer> intersectionNodes = ConcurrentHashMap.newKeySet();

        public void addForwardLabel(int nodeId, Label label) {
            forwardVisited
                .computeIfAbsent(nodeId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(label);
        }

        public void addBackwardLabel(int nodeId, Label label) {
            backwardVisited
                .computeIfAbsent(nodeId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(label);
        }
        
        public void addIntersectionNode(int nodeId) {
            intersectionNodes.add(nodeId); // Set is already concurrent
        }

        public boolean isIntersection(int nodeId) {
            return forwardVisited.containsKey(nodeId) && backwardVisited.containsKey(nodeId);
        }
    }

	public Result driver() throws InterruptedException, ExecutionException {
		Graph.forwardAstar(source, destination, budget);
		
		Graph.backwardAstar(source, destination, budget);

		if(Graph.get_node(source).isFeasible()) {
			SharedState shared = new SharedState();

			shared.backwardVisited.clear();
			shared.forwardVisited.clear();
			shared.intersectionNodes.clear();
			
			List<Double> forward_time_series = new ArrayList<Double>();
			forward_time_series.add(start_departure_time);
			
			List<Double> forward_tmp_time_series = Graph.getTimeSeries(start_departure_time, end_departure_time);
			
			forward_time_series.addAll(forward_tmp_time_series);
			forward_time_series.add(end_departure_time);
			
			List<BreakPoint> forward_arrival_break_points = createArrivalBreakpoints(forward_time_series);
			List<BreakPoint> forward_score_break_points = createScoreBreakpoints(forward_time_series);
			
			Function forward_arrival_time = new Function(forward_arrival_break_points);
			Function forward_score = new Function(forward_score_break_points);
			
			Label sourceLabel = new Label(source, forward_arrival_time, forward_score);
			//sourceLabel.initializeLists();
			sourceLabel.setVisited(source, -1);
			
			BidirectionalLabeling forward_task = new BidirectionalLabeling(destination, budget/2, sourceLabel, shared, true);
			//forward_task.run();
			
			List<Double> backward_time_series = new ArrayList<Double>();
			double fastest_path_cost = Graph.get_node(destination).get_forward_hScore();
			backward_time_series.add(start_departure_time+fastest_path_cost);
			
			List<Double> backward_tmp_time_series = Graph.getTimeSeries(start_departure_time+fastest_path_cost, end_departure_time+budget);
			
			backward_time_series.addAll(backward_tmp_time_series);
			backward_time_series.add(end_departure_time);
			
			List<BreakPoint> backward_arrival_break_points = createArrivalBreakpoints(backward_time_series);
			List<BreakPoint> backward_score_break_points = createScoreBreakpoints(backward_time_series);
			
			Function backward_arrival_time = new Function(backward_arrival_break_points);
			Function backward_score = new Function(backward_score_break_points);
			
			Label destinationLabel = new Label(destination, backward_arrival_time, backward_score);
			//sourceLabel.initializeLists();
			destinationLabel.setVisited(destination, -1);
			BidirectionalLabeling backward_task = new BidirectionalLabeling(source, budget/2, destinationLabel, shared, false);
			//backward_task.run();
			ForkJoinTask<?> forwardFuture = BidirectionalAstar.pool.submit(forward_task);
			ForkJoinTask<?> backwardFuture = BidirectionalAstar.pool.submit(backward_task);
			forwardFuture.join();
			backwardFuture.join();
//			String analysis_file = "Analysis"+index+"_" + Graph.get_vertex_count() +".txt";
//			FileWriter fanalysis = new FileWriter(analysis_file);
//			BufferedWriter writer2 = new BufferedWriter(fanalysis);
//			
//			String path_file = "Path"+index+"_" + Graph.get_vertex_count() +".txt";
//			FileWriter fpath = new FileWriter(path_file);
//			BufferedWriter writer3 = new BufferedWriter(fpath);
//			
//			index++;
			
			//BidirectionalDriver driver = new BidirectionalDriver(queries.peek().get_destination(), budget);
//			ForwardLabeling forwardSolver = new ForwardLabeling(destination, budget, sourceLabel);
//			Map<Integer,List<Label>> forward_labels = forwardSolver.call();
//			forwardSolver.setMaster(); 
//			Map<Integer,Result> pruned_forward_labels = pruneDomination(forward_labels);
			
			
//			BackwardLabeling backwardSolver = new BackwardLabeling(source, budget, destinationLabel);
//			Map<Integer,List<Label>> backward_labels = backwardSolver.call();
			//Map<Integer,Result> pruned_backward_labels = pruneDomination(backward_labels);
			Result result = formOutputLabels(shared.intersectionNodes, shared.forwardVisited, shared.backwardVisited);
			
			return result;
		}
		return null;
	}

	private Result formOutputLabels(Set<Integer> intersectionNodes, Map<Integer, List<Label>> forward_labels, Map<Integer, List<Label>> backward_labels) {
		Result finalResult = null;
		
		for(int current_join_node:intersectionNodes) {
			List<Label> current_backward_labels = backward_labels.get(current_join_node);
			
			for(Label current_backward_label:current_backward_labels) {
				List<Label> current_forward_labels = forward_labels.get(current_join_node);
				
				for(Label current_forward_label:current_forward_labels) {
					Result current_result = getResult(current_forward_label, current_backward_label);
					
					if(finalResult==null || finalResult.get_score()<current_result.get_score()) {
						finalResult=current_result;
					}
					
				}
			}
			
		}
		return finalResult;
	}

	private Result getResult(Label current_forward_label, Label current_backward_label) {
		
		double dep_time = -1;
		double scr = -1;

		
					
//		int current = destination;
//		List<Integer> path = new ArrayList<Integer>();
//		while(!destination_label.getVisitedList().get(current).equals(-1)) {
//			path.add(current);
//		   	current = destination_label.getVisitedList().get(current);
//		}
//
//		path.add(current);
//		Collections.reverse(path);
//		
//		for(int i:path)
//			writer3.write(i+",");
//		writer3.write("\n");
		//writer2.write("[");
		Function forward_score_function = current_forward_label.get_score();
		//Function current_arrival_function = current_forward_label.get_arrivalTime();
		
		while(forward_score_function != null) {
			List<BreakPoint> score_breakpoints = forward_score_function.getBreakpoints();
			//List<BreakPoint> arrival_time_breakpoints = current_arrival_function.getBreakpoints();
			for(int i =0;i<score_breakpoints.size();i++) {
				double forward_score = score_breakpoints.get(i).getY();
				double tmp_dep_time = score_breakpoints.get(i).getX();
				double backward_score = current_backward_label.get_score(tmp_dep_time);
				
				//writer2.write("("+ score_breakpoints.get(i).getX()+","+score_breakpoints.get(i).getY()+"), ");
				if(forward_score+backward_score>scr) {
					
					scr = forward_score+backward_score;
					dep_time = tmp_dep_time;
				}
			}
			
			forward_score_function = forward_score_function.getNextFunction();
		}
		//writer2.write("],\n");
//			/int i= (int) start_departure_time;
//			if(destination_label.get_arrivalTime().getBreakpoints().get(0).getX() >= i)
//				i= (int) Math.ceil(destination_label.get_arrivalTime().getBreakpoints().get(0).getX());
//			
//			int j = (int) end_departure_time;
//			if(destination_label.get_arrivalTime().getBreakpoints().get(destination_label.get_arrivalTime().getBreakpoints().size()-1).getX() <= j) 
//				j= (int) Math.floor(destination_label.get_arrivalTime().getBreakpoints().get(destination_label.get_arrivalTime().getBreakpoints().size()-1).getX());
//			
//			for(; i<=j;i++) {
//				double tmp_arr_time = destination_label.get_arrivalTime(i);//TODO
//				if(tmp_arr_time-i<=budget) {
//					int tmp_score = destination_label.get_score(i);
//					
//					if(tmp_score>scr) {
//						dep_time = i;
//						arr_time = tmp_arr_time;
//						scr = tmp_score;
//					}
//				}
//			}
		//writer2.flush();
		//writer3.flush();
		
		Result result = new Result(dep_time, scr);
		return result;
	}
	
	private static List<BreakPoint> createScoreBreakpoints(List<Double> time_series) {
		List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();
		
		for(double time_point: time_series) {
			BreakPoint break_point = new BreakPoint(time_point, 0);
			breakpoints.add(break_point);
		}
		return breakpoints; 
	}

	private static List<BreakPoint> createArrivalBreakpoints(List<Double> time_series) {
		List<BreakPoint> breakpoints = new ArrayList<BreakPoint>();
		
		for(double time_point: time_series) {
			BreakPoint break_point = new BreakPoint(time_point, time_point);
			breakpoints.add(break_point);
		}
		return breakpoints; 
	}

}
