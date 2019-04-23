package StateActionStateSequencer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import cz.agents.alite.creator.Creator;
import cz.agents.dimaptools.experiment.Trace;

public class StateActionStateSequencer implements Creator {

	/* Global variables */
	private final static Logger LOGGER = Logger.getLogger(StateActionStateSequencer.class);
	private final static int ARGS_NUM = 4;
	
	private static final String MA_TO_AGENT_FILE_SCRIPT = "./PythonScripts/unfactoredMAPDDL-extract-agent-file.py";
	private static final String MA_TO_AGENT_FILE_OUTPUT = "./Output/Agents";

	/* Class variables */
	private String tracePath = "";
	private String domainPath = "";
	private String problemPath = "";
	
	private File traceFile = null;
	private File domainFile = null;
	private File problemFile = null;

	@Override
	public void create() {

		LOGGER.info("StateActionStateSequencer End");

	}
	@Override
	public void init(String[] args) {

		//Trace.setFileStream("Log/trace.log");
		LOGGER.setLevel(Level.INFO);
		
		LOGGER.info("StateActionStateSequencer Start");

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
		
		if(!ReadTraceFile(tracePath))
		{
			LOGGER.fatal("Trace file reading failure");
			System.exit(1);
		}
	}


	private boolean ReadTraceFile(String traceFilePath) {

		return false;
	}
	
	private boolean CreateAgentsFile(String outputPath) {

		LOGGER.info("Agents File creation");

		try {

			String cmd = MA_TO_AGENT_FILE_SCRIPT + " " + domainPath + " " + problemPath + " " + outputPath;

			LOGGER.info("Running: " + cmd);

			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();

		} catch (Exception e) {
			LOGGER.fatal(e, e);
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

		tracePath = args[3];
		traceFile = new File(tracePath);
		
		if( !traceFile.exists()) {
			LOGGER.fatal("provided path to trace file not existing");
			return false;
		}
		
		return true;
	}
}
