/**
 * 
 */

import java.util.HashMap;

/**
 * 
 */
public class Path {

	private HashMap<Integer, Integer> path;
	private	double current_time;
	private	int score;
	private int last_node;
	
	private void update_cost_and_score(int node){
		if(this.path.size()==1) {
			this.score = 0;
			this.last_node = node;
		}
		else {
	        Node current_node = Graph.get_node(this.last_node);
	        Edge edg = current_node.get_outgoing_edges().get(node);
	
	        this.current_time = edg.get_arrival_time(this.current_time);
	        this.score = this.score + edg.get_score(this.current_time);
	        this.last_node = node;
		}
	}

	public double get_cost(){
		return this.current_time;
	}

	public int get_score(){
		return this.score;
	}

	public int get_last_node(){
		return this.last_node;
	}

	public HashMap<Integer, Integer> get_path(){
		return this.path;
	}
	
	public void update_path(int new_node){
		this.path.put(new_node, new_node);
		this.update_cost_and_score(new_node);
	}

	public boolean check_loop(int new_node){
		return this.path.containsKey(new_node);
	}

	public void copy_from(Path source_path) {
		this.current_time = source_path.get_cost();
		this.score = source_path.get_score();
		this.last_node = source_path.get_last_node();
		this.path.putAll(source_path.get_path());
	}

	public Path(double time){
		this.current_time = time;
		this.path = new HashMap<Integer, Integer>();
	}
	
}
