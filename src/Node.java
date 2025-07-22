/**
 * 
 */

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class Node {

	private Map<Integer, Edge> outgoing_edges;
	private	Map<Integer, Edge> incoming_edges;
	private	double latitude;
	private	double longitude;
	private boolean backward_reachebility;
	private double backward_hScore;
	private double forward_hScore;//fastest path cost for forward search
	private boolean forward_reachebility;//fastest path cost for backward search
	private boolean feasible;
	private boolean middle_node;
	private boolean forward_label;
	private boolean backward_label;
	private boolean joint_node;

	public void setForwardHScore(double hScore) {
		this.forward_hScore = hScore;
	}

	public void setBackwardHScore(double hScore) {
		this.backward_hScore = hScore;
	}
	
	public void setBackwardReachebility() {
		this.backward_reachebility = true;
		setFeasibility();
	}

	public void setForwardReachebility() {
		this.forward_reachebility = true;
		setFeasibility();
	}
	
	private void setFeasibility() {
		if(this.forward_reachebility && this.backward_reachebility) {
			this.feasible = true;
			
		}
	}
	
	public void setMiddleNode() {
		this.middle_node=true;
	}
	
	public boolean isFeasible() {
		return this.feasible;
	}

	public boolean isForwardReacheble() {
		return this.forward_reachebility;
	}

	public boolean isBackwardReacheble() {
		return this.backward_reachebility;
	}

	public void setBackwardLabel() {
		this.backward_label = true;
		setJointNode();
	}

	public void setForwardLabel() {
		this.forward_label = true;
		setJointNode();
	}
	
	public void setJointNode() {
		if(this.forward_label && this.backward_label) {
			this.joint_node=true;
		}
	}
	
	
	public boolean isJointNode() {
		return this.joint_node;
	}

	public boolean isForwardLabeled() {
		return this.forward_label;
	}

	public boolean isBackwardLabeled() {
		return this.backward_label;
	}

	public boolean isMiddle() {
		return this.middle_node;
	}

	public double get_backward_hScore() {
		return this.backward_hScore;
	}

	public double get_forward_hScore() {
		return this.forward_hScore;
	}
	
	public void reset() {
		this.backward_reachebility = false;
		this.backward_hScore=Double.MAX_VALUE;
		this.forward_hScore=Double.MAX_VALUE;
		this.forward_reachebility=false;
		this.feasible=false;
		this.middle_node=false;
		this.forward_label=false;
		this.backward_label=false;
		this.joint_node=false;
	}
	
	public double get_latitude(){
		return latitude;
	}

	public double get_longitude(){
		return longitude;
	}

	public void insert_incoming_edge(Edge edge){
		incoming_edges.put(edge.get_source(),edge);
	}

	public void insert_outgoing_edge(Edge edge){
		outgoing_edges.put(edge.get_destination(), edge);
	}

	public Map<Integer, Edge> get_incoming_edges(){
		return incoming_edges;
	}

	public Map<Integer, Edge> get_outgoing_edges(){
		return outgoing_edges;
	}

	public double euclidean_distance(Node node){
		double x1 = latitude;
		double y1 = longitude;
		double x2 = node.get_latitude();
		double y2 = node.get_longitude();

		return Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
	}

	public Node(double lat, double longi){
		this.latitude = lat;
		this.longitude = longi;
		this.backward_reachebility = false;
		this.incoming_edges = new HashMap<Integer, Edge>();
		this.outgoing_edges = new HashMap<Integer, Edge>();
	}

}
