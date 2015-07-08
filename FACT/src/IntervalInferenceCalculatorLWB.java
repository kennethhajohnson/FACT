import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;

import javax.script.ScriptException;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;

public class IntervalInferenceCalculatorLWB {
	/*This class requires programmatic access to MatLab with Yalmip libraries installed.
	 * 1) Download and install Yalmip: http://users.isy.liu.se/johanl/yalmip/
	 * 2) The jar "matlabcontrol-4.1.0.jar"  enables programmatic access to MatLab and is available here:
	 *    https://code.google.com/p/matlabcontrol   
	 *     
	 *    
	 * The Yalmip program used to find global min/max of the requirements is based on the example:
	 * http://users.isy.liu.se/johanl/yalmip/pmwiki.php?n=Tutorials.GlobalOptimization    
	 * */
	static public MatlabProxyFactory matLabFactory;
	static public MatlabProxy proxy;	


	static private String inputDirectory=
			"../FACT/CaseStudies/LowPowerWirelessBus/";

	static private String outputDirectory=
			"../FACT/output/";
	
	
	
	
	
	
	static private String experimentPrefix="output_";	
	private Random rndGen = new Random(System.currentTimeMillis());
	//------------------------------------------------------------------------------------------------
	public static double[] minMax(PrintStream experimentOutputFile,Experiment e)
			throws MatlabConnectionException, MatlabInvocationException{


		StateParameter sp = e.sps.get(0);//specialised case for parameter p
		try {
			
			proxy.eval(new String("minFunction = @(p1) "+e.req));
			proxy.eval(new String("e=fminbnd(minFunction,"+sp.parms.get(0).interval[0]+","+sp.parms.get(0).interval[1]+")"));
			//System.out.println("e=fminbnd(minFunction,"+sp.parms.get(0).interval[0]+","+sp.parms.get(0).interval[1]+")");
			Object[] minElements = proxy.returningEval("double(e)", 1);
			Object minArgument = minElements[0];
			
			double min = evaluate(e.req,((double[]) minArgument)[0]);
			
			if(min <0) min = Math.abs(min);


			proxy.eval(new String("maxFunction = @(p1) "+"-("+e.req+")"));
			proxy.eval(new String("e=fminbnd(maxFunction,"+sp.parms.get(0).interval[0]+","+sp.parms.get(0).interval[1]+")"));
			Object[] maxElements = proxy.returningEval("double(e)", 1);
			Object maxArgument = maxElements[0];
			

			double max = evaluate("-("+e.req+")",((double[]) maxArgument)[0]);
			if(max<0) max=Math.abs(max);

			if(max < min)
				return new double[]{max,min};

			return new double[]{min,max};
		} catch (MatlabInvocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return new double[]{-1,-1};
	}
	//------------------------------------------------------------------------------------------------
	public static double evaluate(String f,double val){
		try {			
			proxy.eval(new String("f = @(p1) "+f));
			proxy.eval(new String("e=feval(f,"+val+")"));
			Object[] elements = proxy.returningEval("double(e)", 1);
			Object e1 = elements[0];
			return ((double[]) e1)[0];				
		} catch (MatlabInvocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	//------------------------------------------------------------------------------------------------------------------------
	private static double[] yalmip(PrintStream experimentOutputFile,Experiment e)
			throws MatlabConnectionException, MatlabInvocationException{
		try {
			String varDefinition="clear all";			
			String F = "\nF = [";
			for(StateParameter sp : e.sps)
				for(TransitionParameter tp : sp.parms){
					varDefinition+="\n"+tp.name+" = sdpvar(1,1)";
					F+="\n"+tp.interval[0]+"<="+tp.name+"<="+tp.interval[1]+",";
				}	

			for(StateParameter sp : e.sps){
				String eq="\n";
				for(TransitionParameter tp : sp.parms)
					eq+=tp.name+"+";

				eq=eq.substring(0,eq.length()-1);//remove last +
				eq+="==1,";
				F+=eq;//add to constraint string
			}

			F=F.substring(0,F.length()-1)+"];";//remove last comma

			String requirement = "\nRmin = "+e.req+"\n"+"Rmax = "+"-("+e.req+")";

			String command = "\noptions = sdpsettings('verbose',1,'solver','bmibnb')"+//Mosek//bmibnb
					"\nsolvesdp(F,Rmin,options)";


			System.out.println(varDefinition+F+requirement+command);
			proxy.eval(varDefinition+F+requirement+command);
			//System.out.println(F);
			Object[] returnArgumentsMin = proxy.returningEval("double(Rmin)", 1);
			//Retrieve the first (and only) element from the returned argumentsx
			Object minArgument = returnArgumentsMin[0];
			//Like before, cast and index to retrieve the double value
			double min = ((double[]) minArgument)[0];
			if (min <0) min=Math.abs(min);			
			proxy.eval("solvesdp(F,Rmax,options)");

			Object[] returnArgumentsMax = proxy.returningEval("double(Rmax)", 1);			
			Object maxArgument = returnArgumentsMax[0];			
			double max = ((double[]) maxArgument)[0];
			if (max <0) max=Math.abs(max);			

			if(max < min)
				return new double[]{max,min};

			return new double[]{min,max};

		} catch (MatlabInvocationException error) {

			error.printStackTrace();
		}
		return new double[0];
	}
	//------------------------------------------------------------------------------------------------
	static ArrayList<Experiment> readInputFile(String filename){	
		ArrayList<Experiment> exps = new ArrayList<Experiment>();		
		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = null;
			
			while ((line = reader.readLine())!=null){
				String description="";
				while (line.contains(";")){//handle comments first.
					description+=line;
					line=reader.readLine();
				}
				
				Experiment e = new Experiment(line,description);			
				String[] tokens = reader.readLine().split(" ");//parse the values for beta,upper,step.
				e.oneMinusBeta = Double.parseDouble(tokens[0]);
				e.upperBound = Double.parseDouble(tokens[1]);
				e.step = Double.parseDouble(tokens[2]);
				StateParameter sp=null;
				while(((line=reader.readLine())!=null)&&(!line.contains("END"))){
					tokens = line.split(" ");
					sp = new StateParameter(tokens[0],Double.parseDouble(tokens[2]),Integer.parseInt(tokens[1]));
					for(int i=0;i<sp.pNum;i++)
						sp.addTP(sp.name+(i+1), Double.parseDouble(reader.readLine()));
					e.addSP(sp);					
				}	
				exps.add(e);
				
			}
			reader.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
		return exps;
	}

	//------------------------------------------------------------------------------------------------
	public static void main(String[] args) throws ScriptException {

		/*
		LWBInputFileGenerator gen = new LWBInputFileGenerator();
		gen.fileName="LWBGeneratorinputExperiment95.txt";
		gen.Tstart=1000;
		gen.Tstop=60000;
		gen.Tstep=10000;
		gen.reqs = new String[]{LWBInputFileGenerator.R1,LWBInputFileGenerator.R2};
		gen.alphaRanges= new String[]{"0.95 0.95 0.01"};
		gen.observations= new String[]{"p 2 10000\n8090\n1910"};
		gen.generate(inputDirectory);
		String[] fileNames = new String[]{gen.fileName};

		*/
		
		LWBInputFileGenerator gen1 = new LWBInputFileGenerator();	
		gen1.fileName="LWBGeneratorinput.txt";
		gen1.Tstart=1000;
		gen1.Tstop=60000;
		gen1.Tstep=1000;
		gen1.reqs = new String[]{LWBInputFileGenerator.R1,LWBInputFileGenerator.R2};
		gen1.alphaRanges= new String[]{"0.90 0.99 0.01"};
		gen1.observations= new String[]{"p 2 1000\n794\n206","p 2 10000\n8090\n1910","p 2 30000\n24021\n5979","p 2 60000\n47987\n12013"};
		gen1.generate(inputDirectory);
		String[] fileNames = new String[]{gen1.fileName};
		
		
		 
		
		
		//sanity check.				
		System.out.println("Checking input files: ");
		for(String aFilename : fileNames){
			File f = new File(inputDirectory+aFilename);
			if(f.exists() && !f.isDirectory()) 
				System.out.println(" "+aFilename+": OK "); else{
					System.out.println("error with: "+aFilename);
					System.exit(-1);
				}
		}
		try {
			matLabFactory = new MatlabProxyFactory();
			try {
				proxy = matLabFactory.getProxy();
			} catch (MatlabConnectionException e1) {				
				e1.printStackTrace();
			}

			for(String filename : fileNames){
				PrintStream experimentOutputFile = new PrintStream(new File(outputDirectory+experimentPrefix+filename));							

				IntervalInferenceCalculatorLWB calculator = new IntervalInferenceCalculatorLWB();
				ArrayList<Experiment> exps= readInputFile(inputDirectory+filename);
				System.out.println("Reading input file: "+inputDirectory+filename);
				System.out.println("Created output file: "+outputDirectory+experimentPrefix+filename);
				for(Experiment e : exps){
					String requirement = e.req;
					double startTime = System.currentTimeMillis();
					System.out.println("\n"+e.description);
					System.out.println("Analysing " + requirement + " for the parameter intervals below:");
					experimentOutputFile.println("\n"+e.description);
					experimentOutputFile.println("\nAnalysing " + requirement + " for the parameter intervals below:");
					// Find confidence interval for a range of confidence levels, e.g., 70% to 90% in steps of 1%
					for (double oneMinusBeta = e.oneMinusBeta; oneMinusBeta <= e.upperBound + 0.00002; oneMinusBeta += e.step){
						for(StateParameter sp : e.sps)
							sp.oneMinusAlpha = Math.pow(oneMinusBeta, 1.0/e.sps.size());//sps.size = 5,3,4,3,3 according to number of state parameters in req.


						double bestWidth = Double.MAX_VALUE;
						double bestMin=Double.MAX_VALUE;
						double bestMax=Double.NEGATIVE_INFINITY;


						// This is the hill climbing that aims to adjust the (1-\alpha_i)'s only do if e.sps > 1
						int nIterations = 0;
						double epsilon =0.005;//= 0.005;
						double Nmax = 50;
						double iterationsWithoutImprovement = 0;

						while ((e.sps.size()>=1 && iterationsWithoutImprovement < Nmax) || nIterations==0) {							
							nIterations++;						
							for(StateParameter sp : e.sps){
								for(TransitionParameter tp: sp.parms){
									double[] tmp = calculator.computeInterval(sp.oneMinusAlpha, tp.transitionObservationCount/sp.totalObservationCount, 
											sp.totalObservationCount);
									if (tmp[0]<0) tmp[0] = 0;
									if (tmp[1]>1) tmp[1] = 1;
									tp.interval[0] = Double.toString(tmp[0]);
									tp.interval[1] = Double.toString(tmp[1]);
									//System.out.println(tp + " interval: "+tp.interval[0]+" "+tp.interval[1]);								
								}
							}
							double min = Double.MAX_VALUE;
							double max = Double.MIN_VALUE;


							try {double[] 
									temp=minMax(experimentOutputFile,e);//yalmip	
							min=temp[0];max=temp[1];	
							//System.out.println("Min ="+min+" Max: "+max);

							double prismValue=evaluate(e.req,0.8);

							if(!((prismValue >= min)&&(prismValue <= max))){
								System.out.println("Warning: "+prismValue+" not in the interval ["+min+","+max+"]");
								experimentOutputFile.println("Warning: "+prismValue+" not in the interval ["+min+","+max+"]");
							}

							} catch (MatlabConnectionException error) {
								error.printStackTrace();
							} catch (MatlabInvocationException error) {
								error.printStackTrace();
							}						
							// Record best combination so far
							double width = max - min;
							if (width >= bestWidth - epsilon)
								iterationsWithoutImprovement++; else
									iterationsWithoutImprovement = 0; // start from 0 if an improvement was found!

							if (bestWidth > width){ 
								bestWidth = width;
								bestMin=min;
								bestMax=max;
							}

							if(e.sps.size()>1){
								double[] alphas = new double[e.sps.size()];
								int j=0;//obtain alpha values from each state parameter
								for(StateParameter sp : e.sps)
									alphas[j++] = sp.oneMinusAlpha;
								calculator.hillClimbing(alphas);

								j=0;//update alpha value in each state parameter 						
								for(StateParameter sp : e.sps)
									sp.oneMinusAlpha=alphas[j++];
							}
							//System.out.println("nIterations, "+nIterations+" min, "+min+" max, "+max);							
						}

						double endTime = System.currentTimeMillis();
						double duration = endTime - startTime;

						System.out.println(oneMinusBeta+", "+bestMin+", "+bestMax+", "+nIterations+", "+duration/1000.0);// on a single line, without any text, in CSV format (for easy plotting)
						experimentOutputFile.println(oneMinusBeta+", "+bestMin+", "+bestMax+", "+nIterations+", "+duration/1000.0);
					}
				}
				experimentOutputFile.close();
			}
			proxy.disconnect();
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		}
	}
	//------------------------------------------------------------------------------------------------------------	
	public double[] computeInterval(double confidence, double parameter, double observations) {
		double result[] = {0.0,0.0};
		double alpha = 1 - confidence;
		// the quantile will be = 1-(alpha/2)
		double zscore= StatisticsConversion.calculateOneSidedZscore(1.0 - (alpha / 2.0));

		//System.out.println("zscore = "+zscore);

		double firstpart = parameter + (zscore*zscore)/(2.0*observations);
		double secondtpart = (zscore*zscore)*Math.sqrt( ( parameter*(1.0-parameter) + (zscore*zscore)/(4.0*observations))/observations );
		double denom = 1.0+(zscore*zscore)/observations;

		double left = (firstpart - secondtpart)/denom;
		double right = (firstpart + secondtpart)/denom;

		result[0] = left;
		result[1] = right;

		//	System.out.println(result[0]+" "+result[1]);

		return result;
	}

	//------------------------------------------------------------------------------------------------------------
	public void hillClimbing(double[] alphas) {
		// Choose a random alphas to modify
		int idx1 = this.rndGen.nextInt(alphas.length);
		int idx2;

		do {
			idx2 = this.rndGen.nextInt(alphas.length);
		} while (idx2==idx1);

		// Choose a "small" value by which to modify alphas[idx1] and alphas[idx2]
		double delta;
		do {
			delta = 0.9 + this.rndGen.nextDouble()/2.5;
		} while (alphas[idx1]*delta > 1 || alphas[idx2]/delta > 1);
		alphas[idx1] *= delta;
		alphas[idx2] /= delta;		
	}
}


