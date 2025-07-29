/**
 * 
 */

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class Function {
	private List<BreakPoint> break_points;
	private Function next_function = null;
	private double max_score;
	
	public Function(List<BreakPoint> breakpoints, double score) {
		break_points = new ArrayList<BreakPoint>();
		break_points.addAll(breakpoints);
		this.max_score = score;
			
	}
	
	public void updateScore(double scr) {
		if(scr>this.max_score)
			this.max_score = scr;
	}

	public List<BreakPoint> getBreakpoints(){
		return this.break_points;
	}
	
	public Function getNextFunction() {
		return this.next_function;
	}
	
	public void setNextFunction(Function function) {
		this.next_function = function;
	}

	public boolean inInterval(double departure_time) {
		if(departure_time>=this.break_points.get(0).getX() && departure_time<=this.break_points.get(this.break_points.size()-1).getX())
			return true;
		return false;
	}

	public double getMaxScore() {
		return this.max_score;
	}
}
