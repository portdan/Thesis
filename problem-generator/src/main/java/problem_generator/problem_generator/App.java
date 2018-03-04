package problem_generator.problem_generator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import problem_generator.file_generator.ADDLGenerator;
import problem_generator.file_generator.PDDLGenerator;

public class App 
{
	private static String timeStamp;
	
	private static ADDLGenerator addl;
	private static PDDLGenerator pddl;

	public static void main( String[] args )
	{
		/*
		DefineProblemName();
		DefineDomain();
		DefineObjects();
		DefineInit();
		DefineGoal();
		 */

		printArgs(args);

		checkArgs(args);
		
		setTimeStamp();
		
		initGenerators();

		createProblemFile();
		
		createAgentFile();

		/*
        String[] a = {
        		"cz.agents.madla.creator.MAPDDLCreator",
        		"mapddl-benchmarks/logistics00/domain.pddl",
        		"mapddl-benchmarks/logistics00/probLOGISTICS-4-0.pddl", 
        		"benchmarks/logistics00/probLOGISTICS-4-0.addl",
        		"PPsaFF-glcl",
        		"-1",
        		"10"
        		};      

        MAPDDLCreator c = new MAPDDLCreator();
        c.init(a);
        c.create();
		 */
	}

	private static void initGenerators() {
		addl = new ADDLGenerator();
		pddl = new PDDLGenerator();
	}

	private static void setTimeStamp() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");	
		timeStamp = dateFormat.format(Calendar.getInstance().getTime());
	}

	private static void checkArgs(String[] args) {
		if (args.length != 1) {
			System.out.println("provided args: " + Arrays.toString(args));
			System.out.println("Usage ...");
			System.exit(1);
		}
	}

	private static void printArgs(String[] args) {
		for(int i=0;i<args.length;i++){
			System.out.println(args[i]);
		}
	}

	private static void createAgentFile() {
		addl.generateADDLFile(timeStamp);
	}

	private static void createProblemFile() {
		pddl.generateFile(timeStamp);
	}

	
}
