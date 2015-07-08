import java.util.ArrayList;

//this class maintains the state transition parameters
public class StateParameter  {
	ArrayList<TransitionParameter> parms;
	int  pNum;//total number of parameters for this state
	double oneMinusAlpha;
	String name;
	double totalObservationCount;//e.g.nx=59547, total observations at state

	public String toString(){
		String out = name+" ";
		for(TransitionParameter tp : parms)
			out+=tp+" ";
		
		return out;
	}
	
	public boolean hasParameter(String name){
		for(TransitionParameter p : parms)
			if(p.name.contentEquals(name))
				return true;			
		return false;
	}
	
	public boolean isEmpty(){
		return this.parms.isEmpty();
	}

	public void remove(String name){
		TransitionParameter pt=null;
		for(TransitionParameter p : parms)
			if(p.name.contentEquals(name)){
				pt=p;
			}

		if(null!=pt) 
			this.parms.remove(pt);

	}

	public StateParameter(String name,double totalObservationCount) {
		this.name = name;
		this.totalObservationCount = totalObservationCount;
		this.parms = new ArrayList<TransitionParameter>();
		this.pNum=0;
	}
	
	public StateParameter(String name,double totalObservationCount,int pNum) {
		this.name = name;
		this.totalObservationCount = totalObservationCount;
		this.parms = new ArrayList<TransitionParameter>();
		this.pNum=pNum;
	}


	public void addTP(String string, double i) {
		TransitionParameter p = new TransitionParameter(string,i); 
		this.parms.add(p);

	}

}
