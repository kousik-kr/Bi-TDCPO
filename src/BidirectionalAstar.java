/**
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
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
	//public static int MAX_SPEED = 2400;
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
	public static boolean forceStop = false;
	
//	private static HashMap<Integer, Integer> subgraphNodes = new HashMap<Integer, Integer>(); 
//	private static HashMap<Integer, Integer> subgraphIndexes = new HashMap<Integer, Integer>(); 
//	public static int subgraphSize = 0;
	
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException{
		//currentDirectory = args[0];
		//String s = "6105";//args[0];
		int n = 264346;//Integer.parseInt(args[1]);
		density = 20;//Integer.parseInt(args[2]);
		overhead = 30;//Double.parseDouble(args[3]);
		no_of_core = 30;//Integer.parseInt(args[4]);
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
		String output_file = "Output_BiTDCPO_" + Graph.get_vertex_count() + ".txt";
		FileWriter fout = new FileWriter(output_file);
		BufferedWriter writer = new BufferedWriter(fout);
		
		
		runtime = Runtime.getRuntime();
		//int index=0;
		while(!queries.isEmpty()){
			forceStop=false;
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
				writer.write(queries.peek().get_source() + "\t" + queries.peek().get_destination() + "\t" + start_departure_time
						+ "\t" + queries.peek().get_budget() + "\t" + output.get_departureTime() + "\t" + output.get_score() + "\t" + (end - start) / 1000F +
						"\t" + (memory_used/(1024*1024)) +  " " + forceStop + "\n");
				writer.flush();
					System.out.println(queries.peek().get_source() + "\t" + queries.peek().get_destination() + "\t" + start_departure_time
							+ "\t" + queries.peek().get_budget() + "\t" + output.get_departureTime() + "\t" + output.get_score() + "\t" + (end - start) / 1000F +
							"\t" + (memory_used/(1024*1024)) +  "\t" + forceStop);
				
			}
			else {
				System.out.println(queries.peek().get_source() + "\t" + queries.peek().get_destination() + "\t" + queries.peek().get_start_departure_time()
						+ "\t" + Graph.get_node(queries.peek().get_destination()).get_forward_hScore() + "\t" + 0 + "\t" + 0 + "\t" + (end - start) / 1000F +
						"\t" + (memory_used/(1024*1024)) +  "\t" + forceStop);
			}
//				writer2.close();
//				fanalysis.close();
//				writer3.close();
//				fpath.close();
			Graph.reset();
			//clearSubgraph();
			
			queries.poll();
		}
//			if(!optimization)
//				optimization = true;
//			
		
			
		writer.close();
		fout.close();
		System.out.println("All query processing is done.");
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
