/**
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

/**
 * 
 */
public class BidirectionalAstar {

	/**
	 * @param args
	 */
	private static final String currentDirectory = System.getProperty("user.dir");	//current directory of the code
	public static int MAX_SPEED = 2400;
	private static Queue<Query> queries = new LinkedList<Query>();
	//public static double departure_time = 0;
	@SuppressWarnings("unused")
	public static ForkJoinPool pool;// = new ForkJoinPool(16);
	public static long start;
	public static Runtime runtime;
	private static long memory_after;
	private static boolean updated_memory;
	public static double TIME_LIMIT;
	private static double overhead;
	private static int density;
	private static int no_of_core;
	public static boolean optimization = true;
	private static double interval_duration;
	public static double THRESHOLD;
	
//	private static HashMap<Integer, Integer> subgraphNodes = new HashMap<Integer, Integer>(); 
//	private static HashMap<Integer, Integer> subgraphIndexes = new HashMap<Integer, Integer>(); 
//	public static int subgraphSize = 0;
	
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException{
		//currentDirectory = args[0];
		//String s = "6105";//args[0];
		int n = 264346;//Integer.parseInt(args[1]);
		density = 20;//Integer.parseInt(args[2]);
		overhead = 30;//Double.parseDouble(args[3]);
		no_of_core = 1;//Integer.parseInt(args[4]);
		TIME_LIMIT = 5;//Double.parseDouble(args[5]);
		interval_duration = 360;//Integer.parseInt(args[6]);
		THRESHOLD = 10;//Integer.parseInt(args[7]);
		pool = new ForkJoinPool(no_of_core);
		Graph.set_vertex_count(n);
		extract_nodes();
		extract_edges();
		create_query_bucket();
		query_processing();
	}
	
	private static void create_query_bucket() throws IOException{
		String query_file = currentDirectory + "/" + "Src-dest_" + Graph.get_vertex_count() +".txt";
		File fin = new File(query_file);
		BufferedReader br = new BufferedReader(new FileReader(fin));
		String line = null;
		while((line = br.readLine()) != null){
			String[] entries = line.split("\t");
			
			Query query = new Query(Integer.parseInt(entries[0]), Integer.parseInt(entries[1]), Double.parseDouble(entries[2]), Double.parseDouble(entries[2])+interval_duration, Double.parseDouble(entries[3]));
			queries.add(query);
		}
		br.close();
	}

	private static void extract_nodes() throws NumberFormatException, IOException{
		String node_file = currentDirectory + "/" + "nodes_" + Graph.get_vertex_count() +".txt";
		File fin = new File(node_file);
		BufferedReader br = new BufferedReader(new FileReader(fin));
		String line = null;
		while((line = br.readLine()) != null){
			String[] entries = line.split(" ");
			
			Node node = new Node(Double.parseDouble(entries[1]), Double.parseDouble(entries[2]));
			Graph.add_node(Integer.parseInt(entries[0]), node);
		}
		br.close();
	}

	private static void extract_edges() throws NumberFormatException, IOException{
		String edge_file = currentDirectory + "/" + "edges_" + Graph.get_vertex_count() + ".txt";
		File fin = new File(edge_file);
		BufferedReader br = new BufferedReader(new FileReader(fin));
		String line;
		String[] time_series = null;

		if((line = br.readLine()) != null){
			time_series = line.split(" ");
		}
		
		Graph.updateTimeSeries(time_series);

		while((line = br.readLine()) != null){
			String[] entries = null;
			entries = line.split(" ");

			int source = Integer.parseInt(entries[0]);
			int destination = Integer.parseInt(entries[1]);
			String travel_cost = entries[2];
			String score = entries[3];
			Edge edge = new Edge(source, destination);

			String[] travel_costs = null;
			String[] scores = null;

			travel_costs = travel_cost.split(",");
			scores = score.split(",");

			for(int i=0;i<travel_costs.length;i++){
				Properties properties = new Properties(Double.parseDouble(travel_costs[i]), Integer.parseInt(scores[i]));
				edge.add_property(Integer.parseInt(time_series[i]), properties);
			}

			Graph.get_node(source).insert_outgoing_edge(edge);
			Graph.get_node(destination).insert_incoming_edge(edge);
		}
		br.close();
	}

	private static void query_processing() throws IOException, InterruptedException, ExecutionException{
		String output_file = "Output_iSCOPE_" + Graph.get_vertex_count() + ".txt";
		FileWriter fout = new FileWriter(output_file);
		BufferedWriter writer = new BufferedWriter(fout);
		
		
		runtime = Runtime.getRuntime();
		//int index=0;
		while(!queries.isEmpty()){
			double start_departure_time = queries.peek().get_start_departure_time();
			
			runtime.gc();
			long memory_before = runtime.totalMemory() - runtime.freeMemory();
			double budget = queries.peek().get_budget()*(1+overhead/100);
			
			start = System.currentTimeMillis();
			BidirectionalDriver driver = new BidirectionalDriver(queries.peek(), budget);
			Result output = driver.driver();
			
			long end = System.currentTimeMillis();
			long memory_used = memory_after - memory_before;
			
			if(output != null) {
//					if (output.get_departureTime()==-1 && optimization) {
//						optimization = false;
//						Graph.reset_blabeling();
//						continue;
//					}
				writer.write(queries.peek().get_source() + " " + queries.peek().get_destination() + " " + start_departure_time
						+ " " + queries.peek().get_budget() + " " + output.get_departureTime() + " " + output.get_score() + " " + (end - start) / 1000F +
						"\t" + (memory_used/(1024*1024)) /*+  " " + solver.getNLabels()*/ + "\n");
				writer.flush();
//					System.out.println(queries.peek().get_source() + " " + queries.peek().get_destination() + " " + queries.peek().get_start_departure_time()
//							+ " " + queries.peek().get_budget() + " " + output.get_departureTime() + " " + output.get_score() + " " + (end - start) / 1000F + 
//							"\t" + (memory_used/(1024*1024))/* + " " + solver.getNLabels()*/);
				
			}
			else {
				writer.write(queries.peek().get_source() + " " + queries.peek().get_destination() + " " + queries.peek().get_start_departure_time()
						+ " " + queries.peek().get_budget() + " " + 0 + " " + 0 + " " + (end - start) / 1000F +
						"\t" + (memory_used/(1024*1024)) /*+  " " + solver.getNLabels()*/ + "\n");
				writer.flush();
				//System.out.println("Timeout!!");
			}
//				writer2.close();
//				fanalysis.close();
//				writer3.close();
//				fpath.close();
		}
//			if(!optimization)
//				optimization = true;
//			
		Graph.reset();
		//clearSubgraph();
		
		queries.poll();
			
		writer.close();
		fout.close();
		System.out.println("All query processing is done.");
	}

private static Result get_result(double start_departure_time, double end_departure_time, double budget, int destination, List<Label> outputLabels) throws IOException {
		
		double dep_time = -1;
		double arr_time = -1;
		double scr = -1;

		for(Label destination_label: outputLabels) {
					
//			int current = destination;
//			List<Integer> path = new ArrayList<Integer>();
//		    while(!destination_label.getVisitedList().get(current).equals(-1)) {
//				path.add(current);
//		    	current = destination_label.getVisitedList().get(current);
//		    }
//
//			path.add(current);
//			Collections.reverse(path);
//			
//			for(int i:path)
//				writer3.write(i+",");
//			writer3.write("\n");
			//writer2.write("[");
			Function current_score_function = destination_label.get_score();
			Function current_arrival_function = destination_label.get_arrivalTime();
			
			while(current_score_function != null) {
				List<BreakPoint> score_breakpoints = current_score_function.getBreakpoints();
				List<BreakPoint> arrival_time_breakpoints = current_arrival_function.getBreakpoints();
				for(int i =0;i<score_breakpoints.size();i++) {
					double tmp_score = score_breakpoints.get(i).getY();
					//writer2.write("("+ score_breakpoints.get(i).getX()+","+score_breakpoints.get(i).getY()+"), ");
					if(tmp_score>scr) {
						dep_time = arrival_time_breakpoints.get(i).getX();
						arr_time = arrival_time_breakpoints.get(i).getY();
						scr = tmp_score;
					}
				}
				
				current_score_function = current_score_function.getNextFunction();
				current_arrival_function = current_arrival_function.getNextFunction();
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
		}
		//writer2.flush();
		//writer3.flush();
		
		Result result = new Result(dep_time, scr);
		return result;
	}

	public static void updateMemory() {
		//runtime.gc();
		memory_after = runtime.totalMemory() - runtime.freeMemory();
		updated_memory = true;
	}

	public static boolean isMemoryUpdated() {
		return updated_memory;
	}
//	public static void updateSubgraph(int n) {
//		subgraphNodes.put(n, subgraphSize);
//		subgraphSize++;
//	}
//	
//	public static void clearSubgraph() {
//		subgraphNodes.clear();
//		subgraphSize = 0;
//	}
//
//	public static int getIndex(int n) {
//		return subgraphNodes.get(n);
//	}
//	
//	public static int getSubgraphNode(int index) {
//		return subgraphIndexes.get(index);
//	}

}
