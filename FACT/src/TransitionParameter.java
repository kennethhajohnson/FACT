
public class TransitionParameter {
	String name;//x1,y3 etc.
	double transitionObservationCount;	
	public String[] interval = new String[]{"a","b"};
	
	public String toString(){
		return name;
	}
	
	public TransitionParameter(String string, double i) {
		this.name= string;
		this.transitionObservationCount = i;
	}	
}
