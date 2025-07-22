/**
 * 
 */

/**
 * 
 */
public class Result {
	private double departure_time;
	private double score;
	
	public Result(double dep_time, double scr) {
		this.departure_time = dep_time;
		this.score = scr;
	}

	public double get_departureTime() {
		return this.departure_time;
	}

	public double get_score() {
		return this.score;
	}

	public void updateScore(double final_score) {
		this.score = final_score;
		
	}
	
}
