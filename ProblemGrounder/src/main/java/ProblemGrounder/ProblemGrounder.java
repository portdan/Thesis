package ProblemGrounder;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

import cz.agents.alite.creator.Creator;
import cz.agents.dimaptools.experiment.Trace;

public class ProblemGrounder implements Creator {

	/* Global variables */
	private final static Logger LOGGER = Logger.getLogger(ProblemGrounder.class);
	private final static int ARGS_NUM = 3;

	private static final String MA_TO_AGENT_FILE_SCRIPT = "./PythonScripts/generate-agent-file.py";
	private static final String MA_TO_AGENT_FILE_OUTPUT = "./Output/Agent";
	private static final String GROUND_PROBLEM_SCRIPT = "./PythonScripts/ground-problem.py";
	private static final String GROUND_PROBLEM_OUTPUT = "./Output/Grounded";
	private static final String LOCAL_VIEW_OUTPUT = "./Output/LocalView";

	/* Class variables */
	//private String tracePath = "";
	private String domainPath = "";
	private String problemPath = "";
	private String agentsPath = "";

	//private File traceFile = null;
	private File domainFile = null;
	private File problemFile = null;
	private File agentsFile = null;

	@Override
	public void create() {

		LOGGER.info("ProblemGrounder end");

	}
	@Override
	public void init(String[] args) {

		Trace.setFileStream("Log/trace.log");

		LOGGER.info("ProblemGrounder start");

		if(!ParseArgs(args))
		{
			LOGGER.fatal("Args not valid");
			System.exit(1);
		}

		if(!CreateAgentsFile(MA_TO_AGENT_FILE_OUTPUT))
		{
			LOGGER.fatal("Agents file creation failure");
			System.exit(1);
		}

		if(!GroundProblem(GROUND_PROBLEM_OUTPUT,LOCAL_VIEW_OUTPUT))
		{
			LOGGER.fatal("Domain file grounding failure");
			System.exit(1);
		}
	}


	private boolean GroundProblem(String outputPath, String localViewOutput) {

		LOGGER.info("Grounding problem ");

		try {

			String cmd = GROUND_PROBLEM_SCRIPT + " " + domainPath + " " + problemPath + " " + agentsPath + " " + outputPath + " " + localViewOutput;

			LOGGER.info("Running: " + cmd);

			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();

		} catch (Exception e) {
			LOGGER.fatal(e, e);
			return false;
		}

		return true;
	}

	private boolean CreateAgentsFile(String outputPath) {

		LOGGER.info("Agents file creation");

		try {

			String cmd = MA_TO_AGENT_FILE_SCRIPT + " " + domainPath + " " + problemPath + " " + outputPath;

			LOGGER.info("Running: " + cmd);

			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();

		} 
		catch (Exception e) {
			LOGGER.fatal(e, e);
			return false;
		}

		return agentsFileValid(outputPath);
	}
	private boolean agentsFileValid(String outputPath) {

		LOGGER.info("Agents file check");	
		
		String problemName = problemFile.getName();
		
		problemName = problemName.substring(0, problemName.lastIndexOf('.'));

		agentsPath = outputPath + "/" + problemName + ".agents";
		agentsFile = new File(agentsPath);

		if( !agentsFile.exists()) {
			LOGGER.fatal("agents file generation failure");
			return false;
		}

		return true;
	}

	private boolean ParseArgs(String[] args) {

		LOGGER.info("Args validity check");

		String status = "";
		boolean valid = true;

		if ((valid = ArgsLenghtValid(args)) == false) 
			status = "Usage: <path to .pddl domain file> <path to .pddl problem file> <path to .plan file>";	

		if ((valid = ArgsParsingValid(args)) == false) 
			status = "Bad path to one or more provided files";	

		if (!valid) {
			LOGGER.fatal("provided args: " + Arrays.toString(args));
			LOGGER.fatal(status);
			return false;
		}

		return true;
	}

	private boolean ArgsLenghtValid(String[] args) {

		LOGGER.info("Args lenght check");

		return args.length == ARGS_NUM;
	}

	private boolean ArgsParsingValid(String[] args) {

		LOGGER.info("Args parse check");

		domainPath = args[1];
		domainFile = new File(domainPath);

		if( !domainFile.exists()) {
			LOGGER.fatal("provided path to domain file not existing");
			return false;
		}

		problemPath = args[2];
		problemFile = new File(problemPath);

		if( !problemFile.exists()) {
			LOGGER.fatal("provided path to problem file not existing");
			return false;
		}

		/*
		tracePath = args[3];
		traceFile = new File(tracePath);

		if( !traceFile.exists()) {
			LOGGER.fatal("provided path to trace file not existing");
			return false;
		}		
		 */
		return true;
	}
}
