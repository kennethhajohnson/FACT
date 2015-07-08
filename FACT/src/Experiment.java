import java.util.ArrayList;

public class Experiment {
	public String req;
	public String description;
	public double oneMinusBeta;
	public double upperBound;
	public double step;
	public ArrayList<StateParameter> sps = new ArrayList<StateParameter>();	
	public String toString(){
		return this.description;
	}
	//---------------------------------------------------------------------------------	
	public Experiment(String req,String description) {
		this.req = req;
		this.description=description;
		this.sps = new ArrayList<StateParameter>();
	}
	//---------------------------------------------------------------------------------
	public void addSP(StateParameter sp) {
		this.sps.add(sp);	
	}

}
