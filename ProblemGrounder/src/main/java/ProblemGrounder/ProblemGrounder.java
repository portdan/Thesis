package ProblemGrounder;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import Configuration.ProblemGrounderConfiguration;
import Configuration.ConfigurationManager;
import cz.agents.alite.creator.Creator;

public class ProblemGrounder implements Creator {

	/* Global variables */
	private final static Logger LOGGER = Logger.getLogger(ProblemGrounder.class);

	private final static int ARGS_NUM = 2;

	private static final String MA_TO_AGENT_FILE_SCRIPT = "generate-agent-file.py";
	private static final String GROUND_PROBLEM_SCRIPT = "ground-problem.py";

	/* Class variables */
	private String configurationFilePath = "";

	//private File traceFile = null;
	private File domainFile = null;
	private File problemFile = null;
	private File agentsFile = null;

	private String agentOutputFilePath = "";
	private String groundedOutputFilePath = "";
	private String localViewOutputFilePath = "";
	private String pythonScriptsPath = "";

	@Override
	public void create() {

		LOGGER.info("ProblemGrounder end");

	}
	@Override
	public void init(String[] args) {

		//Trace.setFileStream("Log/trace.log");
		LOGGER.setLevel(Level.INFO);

		LOGGER.info("ProblemGrounder start");

		if(!ParseArgs(args))
		{
			LOGGER.fatal("Args not valid");
			System.exit(1);
		}

		if(!ConfigurationManager.getInstance().loadConfiguration(configurationFilePath))
		{
			LOGGER.fatal("Configuration loading failure");
			System.exit(1);
		}

		if(!ApplyConfiguration(ConfigurationManager.getInstance().getCurrentConfiguration()))
		{
			LOGGER.fatal("Configuration setting failure");
			System.exit(1);
		}

		if(!CreateAgentsFile(agentOutputFilePath))
		{
			LOGGER.fatal("Agents file creation failure");
			System.exit(1);
		}

		if(!GroundProblem(groundedOutputFilePath,localViewOutputFilePath))
		{
			LOGGER.fatal("Domain file grounding failure");
			System.exit(1);
		}
	}


	private boolean ApplyConfiguration(ProblemGrounderConfiguration configuration) {

		LOGGER.info("Applying configuration");

		domainFile = new File(configuration.domainPath);
		if( !domainFile.exists()) {
			LOGGER.fatal("provided path to domain file not existing");
			return false;
		}

		problemFile = new File(configuration.problemPath);
		if( !problemFile.exists()) {
			LOGGER.fatal("provided path to problem file not existing");
			return false;
		}

		agentOutputFilePath = configuration.agentsOutputPath;
		groundedOutputFilePath = configuration.groundedOutputPath;
		localViewOutputFilePath = configuration.localViewOutputPath;

		pythonScriptsPath = configuration.pythonScriptsPath;

		return true;
	}


	private boolean GroundProblem(String outputPath, String localViewOutput) {

		LOGGER.info("Grounding problem ");

		try {

			ProblemGrounderConfiguration conf = ConfigurationManager.getInstance().getCurrentConfiguration();

			String scriptPath = pythonScriptsPath + "/" + GROUND_PROBLEM_SCRIPT;

			String cmd = scriptPath + " " + conf.domainPath + " " + conf.problemPath + " " + agentsFile.getPath() + " " + outputPath + " " + localViewOutput;

			LOGGER.info("Running: " + cmd);

			ProcessBuilder pb = new ProcessBuilder(scriptPath, conf.domainPath, conf.problemPath, agentsFile.getPath(), outputPath, localViewOutput);
            pb.redirectOutput(Redirect.INHERIT);
            
            Process pr = pb.start();

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

			ProblemGrounderConfiguration conf = ConfigurationManager.getInstance().getCurrentConfiguration();

			String scriptPath = pythonScriptsPath + "/" + MA_TO_AGENT_FILE_SCRIPT;

			String cmd = "python " + scriptPath + " " + conf.domainPath + " " + conf.problemPath + " " + outputPath;

			LOGGER.info("Running: " + cmd);
			
			ProcessBuilder pb = new ProcessBuilder(scriptPath, conf.domainPath, conf.problemPath, outputPath);
            pb.redirectOutput(Redirect.INHERIT);
            
            Process pr = pb.start();

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

		String agentsFilePath = outputPath + "/" + problemName + ".agents";

		LOGGER.info("agents file path :" + agentsFilePath);

		agentsFile = new File(agentsFilePath);

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
			status = "Usage: <path to .json configuration file>";	

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

		configurationFilePath = args[1];
		File configurationFile = new File(configurationFilePath);

		if( !configurationFile.exists()) {
			LOGGER.fatal(".json configuration file not exists");
			return false;
		}

		return true;
	}
}
