
import java.io.File;
import java.io.PrintStream;
import java.io.IOException;


public class LWBInputFileGenerator {

	public String[] reqs;
	public String[] alphaRanges;
	public String[] observations;
	public int Tstart;
	public  int Tstop;
	public  int Tstep;
	public String fileName;

	static public String inputDirectory=
			"/Users/kjohnson/Google Drive/Model Learning Paper/Code/LWB case study/input/";

	static public String R1 =
			"( 12*p1^10 - 427*p1^9 + (T+2322)*p1^8 - (6*T+5972)*p1^7 + (16*T+9815)*p1^6 - "
					+ "(24*T+12027)*p1^5 +  (22*T+11102)*p1^4 - (14*T+6277)*p1^3 + (8*T+2455)*p1^2 - "
					+ "(4*T+355)*p1 + T  ) / ( 2*T*p1^8   - 12*T*p1^7 + 34*T*p1^6 - 54*T*p1^5 + 52*T*p1^4 - 32*T*p1^3 + 18*T*p1^2 - 8*T*p1 + 2*T  )"; 

	static public String R2 = "( -1000*p1^3 + 1035*p1^2 - 355*p1 + T  ) / (p1^3 )"; 



	public void generate(String inputFileLocation){
		System.out.println("Generating Experimental Results");
		int count=0;
		try {			
			PrintStream out = new PrintStream(new File(inputFileLocation+this.fileName));
			for(String requirement : this.reqs){				
				for( int T = this.Tstart;T <= this.Tstop;T+=Tstep){
					String TsubRequirement = requirement.replace("T",T+"");
					for(String alpha : this.alphaRanges){
						for(String observation : this.observations){							
							//write a comment for the entry.
							out.println(";experiment: "+count);
							out.println(";T = : "+T);
							out.println(";alpha range = : "+alpha);
							out.println(";observations: "+observation.replace("\n", " "));
							//now write the entry							
							out.println(TsubRequirement);
							out.println(alpha);
							out.println(observation);
							out.println("END");
							count++;
						}
					}

				}
			}
			System.out.println("Generated "+count+" experiments in file: "+this.fileName);
			out.close();	
		} catch (IOException e) {			
			e.printStackTrace();
		}	
		
	}




}
