import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

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

	public Result driver() throws InterruptedException, ExecutionException {
		Graph.forwardAstar(source, destination, budget);
		Graph.backwardAstar(source, destination, budget);

		if(Graph.get_node(source).isFeasible()) {
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
		

			List<Double> backward_time_series = new ArrayList<Double>();
			backward_time_series.add(start_departure_time+budget);
			
			List<Double> backward_tmp_time_series = Graph.getTimeSeries(start_departure_time+budget, end_departure_time+budget);
			
			backward_time_series.addAll(backward_tmp_time_series);
			backward_time_series.add(end_departure_time);
			
			List<BreakPoint> backward_arrival_break_points = createArrivalBreakpoints(backward_time_series);
			List<BreakPoint> backward_score_break_points = createScoreBreakpoints(backward_time_series);
			
			Function backward_arrival_time = new Function(backward_arrival_break_points);
			Function backward_score = new Function(backward_score_break_points);
			
			Label destinationLabel = new Label(source, backward_arrival_time, backward_score);
			//sourceLabel.initializeLists();
			destinationLabel.setVisited(destination, -1);
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
			ForwardLabeling forwardSolver = new ForwardLabeling(destination, budget, sourceLabel);
			Map<Integer,List<Label>> forward_labels = forwardSolver.call();
			forwardSolver.setMaster(); 
			Map<Integer,Result> pruned_forward_labels = pruneDomination(forward_labels);
			
			
			BackwardLabeling backwardSolver = new BackwardLabeling(source, budget, destinationLabel);
			Map<Integer,List<Label>> backward_labels = backwardSolver.call();
			Map<Integer,Result> pruned_backward_labels = pruneDomination(backward_labels);
			
			return formOutputLabels(pruned_forward_labels, pruned_backward_labels);
		}
		return null;
	}


	private Map<Integer, Result> pruneDomination(Map<Integer, List<Label>> forward_labels) {
		// TODO Auto-generated method stub
		return null;
	}

	private Result formOutputLabels(Map<Integer, Result> pruned_forward_labels, Map<Integer, Result> pruned_backward_labels) {
		Result finalResult = null;
		
		for(Entry<Integer,Result> forward_entry:pruned_forward_labels.entrySet()) {
			int current_join_node = forward_entry.getKey();
			Result current_forward_result = forward_entry.getValue();
			Result current_backward_result = pruned_backward_labels.get(current_join_node);
			double final_score = current_forward_result.get_score()+current_backward_result.get_score();
			
			if(finalResult==null || finalResult.get_score()<final_score) {
				current_forward_result.updateScore(final_score);
				finalResult=current_forward_result;
			}
			
		}
		return finalResult;
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
