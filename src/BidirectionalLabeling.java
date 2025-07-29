import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ForkJoinTask;


public class BidirectionalLabeling implements Runnable{
	private int goal;
	private Label topLabel;
	private double budget;
	BidirectionalDriver.SharedState shared;
	private boolean isForward;
	private boolean master = false;
	
	public BidirectionalLabeling(int goal, double b, Label label, BidirectionalDriver.SharedState shared, boolean is_forward){
		this.goal = goal;
		this.topLabel = label;
		this.budget = b;
		this.shared = shared;
		this.isForward = is_forward;
	}

	@Override
	public void run(){

//		List<Label> destinationLabels = new ArrayList<Label>();
		
		long current = System.currentTimeMillis();
		if((current-BidirectionalAstar.start)/1000F >BidirectionalAstar.TIME_LIMIT) {
			if(!BidirectionalAstar.isMemoryUpdated()) 
				BidirectionalAstar.updateMemory();
			BidirectionalAstar.forceStop=true;
			return;
		}
	
		List<ForkJoinTask<?>> labelQueue = new ArrayList<ForkJoinTask<?>>();
		
		//double current_time = topLabel.get_arrivalTime();
		//int current_score = topLabel.get_score();
		int current_vertex = topLabel.get_nodeID();
		
		Node node = Graph.get_node(current_vertex);
		
		if(isForward) {
			
			Map<Integer, Edge> temp_outgoing_edge = node.get_outgoing_edges();
			
			for(Entry<Integer, Edge> entry : temp_outgoing_edge.entrySet()) {
				Edge edge = entry.getValue();
				int j = edge.get_destination();
	//			if(j==destination) {
	//				System.out.println("hi");
	//			}
				if(Graph.get_node(j).isFeasible() && Graph.get_node(j).get_forward_hScore()<=budget && !topLabel.getVisited(j)) {
					Function current_arrivaltime_function = topLabel.get_arrivalTime();//current function at node i
					Function current_score_function = topLabel.get_score();
					
					List<BreakPoint> arrival_time_breakpoints = new ArrayList<BreakPoint>();//to store the breakpoints at node j
					List<BreakPoint> score_breakpoints = new ArrayList<BreakPoint>();
					double max_score = 0;
					
					Function arrivalTime = null;//function to form label at j
					Function score = null; 
					Function currentArrivalTime = null;//function to store new function after split at j
					Function currentScore = null; 
					
					while(current_arrivaltime_function!=null) {
						
						boolean previous_status = false;//previous breakpoint's status: true - within budget, false - outside budget
						boolean is_first = true;
						
						
						for(int time_point=0;time_point<current_arrivaltime_function.getBreakpoints().size();time_point++) {
							
							//breakpoints of current function at i
							BreakPoint arrival_time_breakpoint = current_arrivaltime_function.getBreakpoints().get(time_point);
							BreakPoint score_breakpoint = current_score_function.getBreakpoints().get(time_point);
							
							double current_time = arrival_time_breakpoint.getY();
							double new_arrival_time = edge.get_arrival_time(current_time);
							double new_score;
							
							//to reach j to d
							double min_required_budget = Graph.get_node(j).get_backward_hScore();
							//new breakpoints at node j
							BreakPoint new_arrival_breakpoint = new BreakPoint(arrival_time_breakpoint.getX(), new_arrival_time);
							if(new_arrival_time<0) {
								System.out.println("Hi");
							}
							if((new_arrival_time - arrival_time_breakpoint.getX())<=budget && (new_arrival_time + min_required_budget - arrival_time_breakpoint.getX())<=2*budget)	{
								new_score = score_breakpoint.getY() + edge.get_score(current_time);
								BreakPoint new_score_breakpoint = new BreakPoint(score_breakpoint.getX(), new_score);
								
								
								if(!previous_status){//outside to inside....starting new function
									previous_status = true;
									if(!is_first) {
										double x1 = current_arrivaltime_function.getBreakpoints().get(time_point-1).getX();
										double y1 = edge.get_arrival_time(current_arrivaltime_function.getBreakpoints().get(time_point-1).getY());
										double x2 = new_arrival_breakpoint.getX();
										double y2 = new_arrival_breakpoint.getY(); 
										
										//TODO verify all
										double allotted_budget = (min_required_budget>budget) ? 2*budget - min_required_budget : budget; 
										BreakPoint boundary_breakpoint = computeBoundaryBreakpoint(x1, y1, x2, y2, allotted_budget);
										arrival_time_breakpoints.add(boundary_breakpoint);
										
										double tmp_score = current_score_function.getBreakpoints().get(time_point-1).getY() + edge.get_score(current_arrivaltime_function.getBreakpoints().get(time_point-1).getY());
										BreakPoint boundary_score = new BreakPoint(boundary_breakpoint.getX(), tmp_score);
										score_breakpoints.add(boundary_score);
										
										if(boundary_score.getY()>max_score)
											max_score = boundary_score.getY();
									}
									else {
										is_first = false;
									}
								}
								
								arrival_time_breakpoints.add(new_arrival_breakpoint);
								score_breakpoints.add(new_score_breakpoint);
								
								if(new_score_breakpoint.getY()>max_score)
									max_score = new_score_breakpoint.getY();
								
							}
							else if(is_first || !previous_status) {
								if(is_first)
									is_first = false;
								continue;
							}
							else {
								previous_status = false;
								//TODO inside to outside....split function
								double x1 = arrival_time_breakpoints.get(arrival_time_breakpoints.size()-1).getX();
								double y1 = arrival_time_breakpoints.get(arrival_time_breakpoints.size()-1).getY();
								double x2 = new_arrival_breakpoint.getX();
								double y2 = new_arrival_breakpoint.getY();
								
								//TODO verify all
								double allotted_budget = (min_required_budget>budget) ? 2*budget - min_required_budget : budget; 
								BreakPoint boundary_breakpoint = computeBoundaryBreakpoint(x1, y1, x2, y2, allotted_budget);
								arrival_time_breakpoints.add(boundary_breakpoint);
								double tmp_score = score_breakpoints.get(score_breakpoints.size()-1).getY() + edge.get_score(boundary_breakpoint.getY());
								BreakPoint boundary_score = new BreakPoint(boundary_breakpoint.getX(), tmp_score);
								score_breakpoints.add(boundary_score);
								
								if(boundary_score.getY()>max_score)
									max_score = boundary_score.getY();
								
								computeAndUpdateBreakpoints(arrival_time_breakpoints, score_breakpoints, j, max_score);
								if(arrivalTime==null) {
									arrivalTime = new Function(arrival_time_breakpoints, -1);
									score = new Function(score_breakpoints, max_score);
									currentArrivalTime = arrivalTime;
									currentScore = score;
								}
								else {
									Function newArrivalTime = new Function(arrival_time_breakpoints, -1);
									Function newScore = new Function(score_breakpoints, max_score);
									currentArrivalTime.setNextFunction(newArrivalTime);
									currentScore.setNextFunction(newScore);
									arrivalTime.updateScore(max_score);
									
									currentArrivalTime = newArrivalTime;
									currentScore = newScore;
								}
								arrival_time_breakpoints.clear();
								score_breakpoints.clear();
								max_score=0;
							}
		
						}
					//if((!IntervalCPO.optimization && arrival_time_breakpoints.size()>1) || (IntervalCPO.optimization && arrival_time_breakpoints.size()==topLabel.get_arrivalTime().getBreakpoints().size())) {
						if(arrival_time_breakpoints.size()>0) {	
							computeAndUpdateBreakpoints(arrival_time_breakpoints, score_breakpoints, j, max_score);
							
							if(arrivalTime==null) {
								arrivalTime = new Function(arrival_time_breakpoints, -1);
								score = new Function(score_breakpoints, max_score);
								currentArrivalTime = arrivalTime;
								currentScore = score;
							}
							else {
								Function newArrivalTime = new Function(arrival_time_breakpoints, -1);
								Function newScore = new Function(score_breakpoints, max_score);
								currentArrivalTime.setNextFunction(newArrivalTime);
								currentScore.setNextFunction(newScore);
								
								arrivalTime.updateScore(max_score);
								
								currentArrivalTime = newArrivalTime;
								currentScore = newScore;
							}
						}
						
						
						current_arrivaltime_function = current_arrivaltime_function.getNextFunction();
						current_score_function = current_score_function.getNextFunction();
					}
					if(arrivalTime!=null) {
						Label newLabel = new Label(j, arrivalTime, score);
						newLabel.copyLists(topLabel.getVisitedList());//, topLabel.getPredecessorList());, topLabel.getTrace()
						newLabel.setVisited(j, current_vertex);
						//newLabel.setPredecessor(j, current_vertex);
//						if(shared.forwardVisited.containsKey(j)) {
//							shared.forwardVisited.get(j).add(newLabel);
//						}
//						else {
//							List<Label> label_list = new ArrayList<Label>();
//							label_list.add(newLabel);
							shared.addForwardLabel(j, newLabel);
//						}
						if(shared.isIntersection(j)) {
							shared.addIntersectionNode(j);
						}
						if(j!=goal) {
							BidirectionalLabeling newthread = new BidirectionalLabeling(goal, budget, newLabel, shared, isForward);
							ForkJoinTask<?> task = BidirectionalAstar.pool.submit(newthread);
							labelQueue.add(task);
							//newthread.run();
						}
						//newthread.fork();
					}
				}
			}
		}
		else {
			Map<Integer, Edge> temp_incoming_edge = node.get_incoming_edges();
			
			for(Entry<Integer, Edge> entry : temp_incoming_edge.entrySet()) {
				Edge edge = entry.getValue();
				int j = edge.get_source();
	//			if(j==destination) {
	//				System.out.println("hi");
	//			}
				if(Graph.get_node(j).isFeasible() && Graph.get_node(j).get_backward_hScore()<=budget && !topLabel.getVisited(j)) {
					Function current_arrivaltime_function = topLabel.get_arrivalTime();//current function at node i
					Function current_score_function = topLabel.get_score();
					
					List<BreakPoint> arrival_time_breakpoints = new ArrayList<BreakPoint>();//to store the breakpoints at node j
					List<BreakPoint> score_breakpoints = new ArrayList<BreakPoint>();
					double max_score =0;
					
					Function arrivalTime = null;//function to form label at j
					Function score = null; 
					Function currentArrivalTime = null;//function to store new function after split at j
					Function currentScore = null; 
					
					while(current_arrivaltime_function!=null) {
						
						boolean previous_status = false;//previous breakpoint's status: true - within budget, false - outside budget
						boolean is_first = true;
						
						
						for(int time_point=0;time_point<current_arrivaltime_function.getBreakpoints().size();time_point++) {
							
							//breakpoints of current function at i
							BreakPoint arrival_time_breakpoint = current_arrivaltime_function.getBreakpoints().get(time_point);
							BreakPoint score_breakpoint = current_score_function.getBreakpoints().get(time_point);
							
							double current_time = arrival_time_breakpoint.getX();
							double new_departure_time = edge.get_departure_time(current_time);
							double new_score;
							
							//to reach j to d
							double min_required_budget = Graph.get_node(j).get_forward_hScore();
							//new breakpoints at node j
							BreakPoint new_arrival_breakpoint = new BreakPoint(new_departure_time, arrival_time_breakpoint.getY());
							if(new_departure_time<0) {
								System.out.println("Hi");
							}
							if((arrival_time_breakpoint.getY() - new_departure_time)<=budget && (arrival_time_breakpoint.getY() + min_required_budget - new_departure_time)<=2*budget)	{
								new_score = score_breakpoint.getY() + edge.get_score(new_departure_time);
								BreakPoint new_score_breakpoint = new BreakPoint(new_departure_time, new_score);
								
								
								if(!previous_status){//outside to inside....starting new function
									previous_status = true;
									if(!is_first) {
										double x1 = edge.get_departure_time(current_arrivaltime_function.getBreakpoints().get(time_point-1).getX());
										double y1 = current_arrivaltime_function.getBreakpoints().get(time_point-1).getY();
										double x2 = new_arrival_breakpoint.getX();
										double y2 = new_arrival_breakpoint.getY(); 
										
										//TODO verify all
										double allotted_budget =  (min_required_budget>budget) ? 2*budget - min_required_budget : budget; 
										BreakPoint boundary_breakpoint = computeBoundaryBreakpoint(x1, y1, x2, y2, allotted_budget);
										arrival_time_breakpoints.add(boundary_breakpoint);
										
										double tmp_score = current_score_function.getBreakpoints().get(time_point-1).getY() + edge.get_score(boundary_breakpoint.getX());
										BreakPoint boundary_score = new BreakPoint(boundary_breakpoint.getX(), tmp_score);
										score_breakpoints.add(boundary_score);
										
										if(boundary_score.getY()>max_score)
											max_score = boundary_score.getY();
										
									}
									else {
										is_first = false;
									}
								}
								
								arrival_time_breakpoints.add(new_arrival_breakpoint);
								score_breakpoints.add(new_score_breakpoint);
								
								if(new_score_breakpoint.getY()>max_score)
									max_score = new_score_breakpoint.getY();
								
								
								
							}
							else if(is_first || !previous_status) {
								if(is_first)
									is_first = false;
								continue;
							}
							else {
								previous_status = false;
								//TODO inside to outside....split function
								double x1 = arrival_time_breakpoints.get(arrival_time_breakpoints.size()-1).getX();
								double y1 = arrival_time_breakpoints.get(arrival_time_breakpoints.size()-1).getY();
								double x2 = new_arrival_breakpoint.getX();
								double y2 = new_arrival_breakpoint.getY();
								
								//TODO verify all
								double allotted_budget = (min_required_budget>budget) ? 2*budget - min_required_budget : budget; 
								BreakPoint boundary_breakpoint = computeBoundaryBreakpoint(x1, y1, x2, y2, allotted_budget);
								arrival_time_breakpoints.add(boundary_breakpoint);
								double tmp_score = score_breakpoints.get(score_breakpoints.size()-1).getY() + edge.get_score(boundary_breakpoint.getX());
								BreakPoint boundary_score = new BreakPoint(boundary_breakpoint.getX(), tmp_score);
								score_breakpoints.add(boundary_score);
								
								if(boundary_score.getY()>max_score)
									max_score = boundary_score.getY();
								
								
								computeAndUpdateBreakpoints(arrival_time_breakpoints, score_breakpoints, j, max_score);
								if(arrivalTime==null) {
									arrivalTime = new Function(arrival_time_breakpoints, -1);
									score = new Function(score_breakpoints, max_score);
									currentArrivalTime = arrivalTime;
									currentScore = score;
								}
								else {
									Function newArrivalTime = new Function(arrival_time_breakpoints, -1);
									Function newScore = new Function(score_breakpoints, max_score);
									currentArrivalTime.setNextFunction(newArrivalTime);
									currentScore.setNextFunction(newScore);
									arrivalTime.updateScore(max_score);
									currentArrivalTime = newArrivalTime;
									currentScore = newScore;
								}
								arrival_time_breakpoints.clear();
								score_breakpoints.clear();
								max_score=0;
							}
		
						}
					//if((!IntervalCPO.optimization && arrival_time_breakpoints.size()>1) || (IntervalCPO.optimization && arrival_time_breakpoints.size()==topLabel.get_arrivalTime().getBreakpoints().size())) {
						if(arrival_time_breakpoints.size()>0) {	
							computeAndUpdateBreakpoints(arrival_time_breakpoints, score_breakpoints, j, max_score);
							
							if(arrivalTime==null) {
								arrivalTime = new Function(arrival_time_breakpoints, -1);
								score = new Function(score_breakpoints, max_score);
								currentArrivalTime = arrivalTime;
								currentScore = score;
							}
							else {
								Function newArrivalTime = new Function(arrival_time_breakpoints, -1);
								Function newScore = new Function(score_breakpoints, max_score);
								currentArrivalTime.setNextFunction(newArrivalTime);
								currentScore.setNextFunction(newScore);
								arrivalTime.updateScore(max_score);
								arrivalTime.updateScore(max_score);
								
								currentArrivalTime = newArrivalTime;
								currentScore = newScore;
							}
						}
						
						
						current_arrivaltime_function = current_arrivaltime_function.getNextFunction();
						current_score_function = current_score_function.getNextFunction();
					}
					if(arrivalTime!=null) {
						Label newLabel = new Label(j, arrivalTime, score);
						newLabel.copyLists(topLabel.getVisitedList());//, topLabel.getPredecessorList());, topLabel.getTrace()
						newLabel.setVisited(j, current_vertex);
						//newLabel.setPredecessor(j, current_vertex);
//						if(shared.backwardVisited.containsKey(j)) {
//							shared.backwardVisited.get(j).add(newLabel);
//						}
//						else {
//							List<Label> label_list = new ArrayList<Label>();
//							label_list.add(newLabel);
							shared.addBackwardLabel(j, newLabel);
						//}
						if(shared.isIntersection(j)) {
							shared.addIntersectionNode(j);
						}
						if(j!= goal) {
							BidirectionalLabeling newthread = new BidirectionalLabeling(goal, budget, newLabel, shared, isForward);
							ForkJoinTask<?> task = BidirectionalAstar.pool.submit(newthread);
							labelQueue.add(task);
							//newthread.run();
						}
						//newthread.fork();
					}
				}
			}
		}
		
		if(master) {
			if(!BidirectionalAstar.isMemoryUpdated()) 
				BidirectionalAstar.updateMemory();
		}
		if(labelQueue.size()>0) {
			for(ForkJoinTask<?> task : labelQueue) {
				//while(!x.isDone()) continue;
				task.join();//x.get();
				
			}	
//			if(master) {
//				if(!BidirectionalAstar.isMemoryUpdated()) 
//					BidirectionalAstar.updateMemory();
//			}
			//nodeWiselabels.clear();
			labelQueue.clear();
		}
	}
	
	public void setMaster() {
		this.master = true;
	}
	
	private BreakPoint computeBoundaryBreakpoint(double x1, double y1, double x2, double y2, double allotted_budget) {
		double x = (allotted_budget+x1*((y2-y1)/(x2-x1))-y1)/(-1+(y2-y1)/(x2-x1));
		double y = -x1*(y2-y1)/(x2-x1) + y1 + ((y2-y1)/(x2-x1))*x;
		if(x<0) {
			System.out.println("Hi");
		}
		return new BreakPoint(x, y);
	}

//
//	public Label get_result() {
//		return result;
//	}

	private void computeAndUpdateBreakpoints(List<BreakPoint> arrival_time_breakpoints,
			List<BreakPoint> score_breakpoints, int next_vertex, double max_score) {
		if(isForward) {
			List<Double> time_series = Graph.getTimeSeries(arrival_time_breakpoints.get(0).getY(), 
					arrival_time_breakpoints.get(arrival_time_breakpoints.size()-1).getY());
			
			List<BreakPoint> tmp_arrival_time_breakpoints = new ArrayList<BreakPoint>();
			List<BreakPoint> tmp_score_breakpoints = new ArrayList<BreakPoint>();
			
			int i = 0, j = 0;
	
	        // Merge the lists while both have elements
	        while (i < arrival_time_breakpoints.size() && j < time_series.size()) {
	            if (arrival_time_breakpoints.get(i).getY() <= time_series.get(j)) {
		            	if(tmp_arrival_time_breakpoints.size()>0 && arrival_time_breakpoints.get(i).getX()-tmp_arrival_time_breakpoints.get(tmp_arrival_time_breakpoints.size()-1).getX()<BidirectionalAstar.THRESHOLD) {
		            		if(score_breakpoints.get(i).getY()>tmp_score_breakpoints.get(tmp_score_breakpoints.size()-1).getY())
		            			tmp_score_breakpoints.get(tmp_score_breakpoints.size()-1).updateY(score_breakpoints.get(i).getY());
		            	}else {
			            	tmp_arrival_time_breakpoints.add(arrival_time_breakpoints.get(i));
			            	tmp_score_breakpoints.add(score_breakpoints.get(i));
		            	}
	
	                i++;
	            } 
	            else {
		            	int current_vertex = topLabel.get_nodeID();
		            	int tmp_next_vertex = next_vertex;
		            	
		            	double dep_time = Graph.get_node(tmp_next_vertex).get_incoming_edges().get(current_vertex).get_departure_time(time_series.get(j));
		            	int score = 0;
		            	Map<Integer, Integer> predList = topLabel.getVisitedList();
		            	
		            	while(predList.get(current_vertex)!=-1) {
		            		tmp_next_vertex = current_vertex;
		            		current_vertex = predList.get(tmp_next_vertex);
		            		dep_time = Graph.get_node(tmp_next_vertex).get_incoming_edges().get(current_vertex).get_departure_time(dep_time);
		            		score += Graph.get_node(tmp_next_vertex).get_incoming_edges().get(current_vertex).get_score(dep_time);
		            	}
		            	
		            	if(dep_time - arrival_time_breakpoints.get(i).getX()<BidirectionalAstar.THRESHOLD) {
		            		if(score>score_breakpoints.get(i).getY())
		            			tmp_score_breakpoints.get(tmp_score_breakpoints.size()-1).updateY(score);
		            	}
		            	else {
			            	BreakPoint new_arrival_time_breakpoint = new BreakPoint(dep_time, time_series.get(j));
			            	if(dep_time<0) {
			            		System.out.println("Hi");
			            	}
			            	BreakPoint new_score_breakpoint = new BreakPoint(dep_time, score);
			            	
			            	tmp_arrival_time_breakpoints.add(new_arrival_time_breakpoint);
			            	tmp_score_breakpoints.add(new_score_breakpoint);
		            	}
	                j++;
	            }
	        }
	
	        // Add remaining elements from list1
	        while (i < arrival_time_breakpoints.size()) {
	        	tmp_arrival_time_breakpoints.add(arrival_time_breakpoints.get(i));
	        	tmp_score_breakpoints.add(score_breakpoints.get(i));
	            i++;
	        }
	        
	        arrival_time_breakpoints.clear();
	        arrival_time_breakpoints.addAll(tmp_arrival_time_breakpoints);
	        score_breakpoints.clear();
	        score_breakpoints.addAll(tmp_score_breakpoints);
		}
		else {
			List<Double> time_series = Graph.getTimeSeries(arrival_time_breakpoints.get(0).getX(), 
					arrival_time_breakpoints.get(arrival_time_breakpoints.size()-1).getX());
			
			List<BreakPoint> tmp_arrival_time_breakpoints = new ArrayList<BreakPoint>();
			List<BreakPoint> tmp_score_breakpoints = new ArrayList<BreakPoint>();
			
			int i = 0, j = 0;

	        // Merge the lists while both have elements
	        while (i < arrival_time_breakpoints.size() && j < time_series.size()) {
	            if (arrival_time_breakpoints.get(i).getX() <= time_series.get(j)) {
		            	if(tmp_arrival_time_breakpoints.size()>0 && arrival_time_breakpoints.get(i).getY()-
		            			tmp_arrival_time_breakpoints.get(tmp_arrival_time_breakpoints.size()-1).getY()<BidirectionalAstar.THRESHOLD) {
		            		if(score_breakpoints.get(i).getY()>tmp_score_breakpoints.get(tmp_score_breakpoints.size()-1).getY())
		            			tmp_score_breakpoints.get(tmp_score_breakpoints.size()-1).updateY(score_breakpoints.get(i).getY());
		            	}else {
			            	tmp_arrival_time_breakpoints.add(arrival_time_breakpoints.get(i));
			            	tmp_score_breakpoints.add(score_breakpoints.get(i));
		            	}

	                i++;
	            } 
	            else {
		            	int current_vertex = topLabel.get_nodeID();
		            	int tmp_next_vertex = next_vertex;
		            	
		            	double arr_time = Graph.get_node(tmp_next_vertex).get_outgoing_edges().get(current_vertex).get_arrival_time(time_series.get(j));
		            	int score = 0;
		            	Map<Integer, Integer> successorList = topLabel.getVisitedList();
		            	
		            	if(successorList.get(current_vertex)==null)
		            		
		            		
		            	while(successorList.get(current_vertex)!=-1) {
		            		tmp_next_vertex = current_vertex;
		            		current_vertex = successorList.get(tmp_next_vertex);
		            		score += Graph.get_node(tmp_next_vertex).get_outgoing_edges().get(current_vertex).get_score(arr_time);
		            		arr_time = Graph.get_node(tmp_next_vertex).get_outgoing_edges().get(current_vertex).get_arrival_time(arr_time);
		            	}
		            	
		            	if(arr_time - arrival_time_breakpoints.get(i).getY()<BidirectionalAstar.THRESHOLD) {
		            		if(score>score_breakpoints.get(i).getY())
		            			tmp_score_breakpoints.get(tmp_score_breakpoints.size()-1).updateY(score);
		            	}
		            	else {
			            	BreakPoint new_arrival_time_breakpoint = new BreakPoint(time_series.get(j), arr_time);
			            	if(arr_time<0) {
			            		System.out.println("Hi");
			            	}
			            	BreakPoint new_score_breakpoint = new BreakPoint(time_series.get(j), score);
			            	
			            	tmp_arrival_time_breakpoints.add(new_arrival_time_breakpoint);
			            	tmp_score_breakpoints.add(new_score_breakpoint);
		            	}
	                j++;
	            }
	        }

	        // Add remaining elements from list1
	        while (i < arrival_time_breakpoints.size()) {
	        	tmp_arrival_time_breakpoints.add(arrival_time_breakpoints.get(i));
	        	tmp_score_breakpoints.add(score_breakpoints.get(i));
	            i++;
	        }
	        
	        arrival_time_breakpoints.clear();
	        arrival_time_breakpoints.addAll(tmp_arrival_time_breakpoints);
	        score_breakpoints.clear();
	        score_breakpoints.addAll(tmp_score_breakpoints);
		}
	}
	
//	private boolean checkDomination(Label newLabel, List<Label> labels) {
//		for(Label label:labels) {
//			if((newLabel.get_arrivalTime() > label.get_arrivalTime()) && 
//					(newLabel.get_score() < label.get_score()))
//				return true;
//			
//		}
//		return false;
//	}
}