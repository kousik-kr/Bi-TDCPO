/**
 * 
 */

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;

/**
 * 
 */
public class Graph {

	private static int n_vertexes;
	private static	Map<Integer, Node> adjacency_list = new HashMap<Integer, Node>();
	private static double[] timeSeries;
	
	public static int get_vertex_count(){
		return n_vertexes;
	}
	
	public static void updateTimeSeries(String[] time_series) {
		timeSeries = new double[time_series.length];
		for(int i=0;i<time_series.length;i++) {
			timeSeries[i] = Double.parseDouble(time_series[i]);
		}
		
	}
	
	public static double[] getTimeSeries() {
		return timeSeries;
	}

	public static List<Double> getTimeSeries(double start_departure_time, double end_departure_time) {
		List<Double> time_series = new ArrayList<Double>();
	
		for (double time_point : timeSeries) {
			
			if(time_point==start_departure_time || time_point== end_departure_time)
				continue;
			
            if (time_point > start_departure_time && time_point< end_departure_time) {
                time_series.add(time_point);
            } else if (time_point > end_departure_time) {
                break; // No need to continue as the list is sorted
            }
        }
		
		return time_series;
	}
	
	public static void set_vertex_count(int n){
		n_vertexes = n;
	}

	public static void add_node(int node_id, Node node){
		adjacency_list.put(node_id, node);
	}

	public static Node get_node(int node_id){
		return adjacency_list.get(node_id);
	}
	
	public static void reset() {
		for(Entry<Integer, Node> entry: adjacency_list.entrySet()) {
			entry.getValue().reset();
		}
	}
	
	public static void forwardAstar(int source, int destination, double budget){

		Map<Integer,Double> hScore = new HashMap<Integer, Double>();
		Map<Integer, Double> gScore = new HashMap<Integer, Double>();
		
		PriorityQueue<Integer> pQueue = new PriorityQueue<Integer>(get_vertex_count(), new Comparator<Integer>(){
			@Override
        	public int compare(Integer i, Integer j){
				
                if(hScore.get(i) < hScore.get(j)){
                    return 1;
                }
                else if (hScore.get(i) > hScore.get(j)){
                    return -1;
                }
                return 0;
            }
		});

		
		pQueue.add(source);
		gScore.put(source, 0.0);
		hScore.put(source, get_node(source).euclidean_distance(get_node(destination))/BidirectionalAstar.MAX_SPEED);
		get_node(source).setForwardReachebility();
		//Main.updateSubgraph(destination);
		
		while(!pQueue.isEmpty()) {

			int current_vertex = pQueue.peek();
			Node node = get_node(current_vertex);
			double current_cost = gScore.get(current_vertex);
			
			Map<Integer, Edge> temp_outgoing_edge = node.get_outgoing_edges();
			
			for(Entry<Integer, Edge> entry : temp_outgoing_edge.entrySet()) {
				
				Edge edge = entry.getValue();
				int j = edge.get_destination();
				double cost_j = edge.getLowestCost();
				double g_score = current_cost + cost_j;
				double f_score = get_node(j).euclidean_distance(get_node(destination))/BidirectionalAstar.MAX_SPEED;
				
				if(g_score+f_score <= budget) {
					if(!hScore.containsKey(j)) {
						get_node(j).setForwardReachebility();
						gScore.put(j, g_score);
						hScore.put(j, g_score+f_score);
						if(j!=destination) pQueue.add(j);
					}
					
					else if(gScore.get(j)>g_score) {
						gScore.replace(j, g_score);
						hScore.replace(j, g_score+f_score);
						
					}
				}
			}
			
			pQueue.poll();
		}
		
		for(Entry<Integer,Double> entry: gScore.entrySet()) {
			get_node(entry.getKey()).setForwardHScore(entry.getValue());
		}
		
	}

	public static void backwardAstar(int source, int destination, double budget){

		Map<Integer,Double> hScore = new HashMap<Integer, Double>();
		Map<Integer, Double> gScore = new HashMap<Integer, Double>();
		
		PriorityQueue<Integer> pQueue = new PriorityQueue<Integer>(get_vertex_count(), new Comparator<Integer>(){
			@Override
        	public int compare(Integer i, Integer j){
				
                if(hScore.get(i) < hScore.get(j)){
                    return 1;
                }
                else if (hScore.get(i) > hScore.get(j)){
                    return -1;
                }
                return 0;
            }
		});

		
		if(get_node(destination).isForwardReacheble())pQueue.add(destination);
		gScore.put(destination, 0.0);
		hScore.put(destination, get_node(destination).get_forward_hScore());
		get_node(destination).setBackwardReachebility();
		//Main.updateSubgraph(destination);
		
		while(!pQueue.isEmpty()) {

			int current_vertex = pQueue.peek();
			Node node = get_node(current_vertex);
			double current_cost = gScore.get(current_vertex);
			
			Map<Integer, Edge> temp_incoming_edge = node.get_incoming_edges();
			
			for(Entry<Integer, Edge> entry : temp_incoming_edge.entrySet()) {
				
				Edge edge = entry.getValue();
				int j = edge.get_source();
				if(!get_node(j).isForwardReacheble())
					continue;
				
				double cost_j = edge.getLowestCost();
				double g_score = current_cost + cost_j;
				double f_score = get_node(j).get_forward_hScore();
				
				if(g_score+f_score <= budget) {
					if(!hScore.containsKey(j)) {
						get_node(j).setBackwardReachebility();
						gScore.put(j, g_score);
						hScore.put(j, g_score+f_score);
						if(j!=source) pQueue.add(j);
					}
					
					else if(gScore.get(j)>g_score) {
						gScore.replace(j, g_score);
						hScore.replace(j, g_score+f_score);
						
					}
				}
			}
			
			pQueue.poll();
		}
		
		for(Entry<Integer,Double> entry: gScore.entrySet()) {
			get_node(entry.getKey()).setBackwardHScore(entry.getValue());
		}
		
	}

}
