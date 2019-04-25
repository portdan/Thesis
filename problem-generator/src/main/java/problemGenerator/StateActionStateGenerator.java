package problemGenerator;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import Configuration.ConfigurationManager;
import Configuration.ProblemGeneratorConfiguration;
import cz.agents.alite.creator.Creator;
import cz.agents.dimaptools.experiment.Trace;
import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.input.sas.SASParser;
import cz.agents.dimaptools.input.sas.SASPreprocessor;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;
import problemGenerator.FileGenerator.SASGenerator;
import problemGenerator.RandomWalker.StateActionStateRandomWalker;

public class StateActionStateGenerator implements Creator {

	/* Global variables */
	private final static Logger LOGGER = Logger.getLogger(StateActionStateGenerator.class);
	private final static int ARGS_NUM = 2;

	private static final String TRANSLATOR = "translate/translate.py";
	private static final String CONVERTOR = "convert/ma-pddl/ma-to-pddl.py";

	private String tempDirPath = "";
	private String tracesDirPath = "";

	/* Class variables */
	private String configurationFilePath = "";

	//private File traceFile = null;
	private File domainFile = null;
	private File problemFile = null;
	private File agentsFile = null;

	private String domainFilePath = "";
	private String problemFilePath = "";
	private String agentsFilePath = "";
	private String pythonScriptsPath = "";


	private String domainFileName;
	private String problemFileName;

	private File sasFile;
	private String sasFilePath;

	private int numOfRandomWalkSteps;
	private int numOfTracesToGenerate;

	private ADDLObject addlParser;

	private SASParser sasParser;
	private SASPreprocessor preprocessor;

	private final List<Problem> problems = new ArrayList<Problem>();

	private Map<String, List<StateActionState>> newStateActionStates = new HashMap<String, List<StateActionState>>();

	@Override
	public void init(String[] args) {

		//Trace.setFileStream("Log/trace.log");
		LOGGER.setLevel(Level.INFO);
		
		LOGGER.info("StateActionStateGenerator start");

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

	}

	private boolean ApplyConfiguration(ProblemGeneratorConfiguration configuration) {

		LOGGER.info("Applying configuration");

		domainFile = new File(configuration.domainFilePath);
		if( !domainFile.exists()) {
			LOGGER.fatal("provided path to domain file not existing");
			return false;
		}

		problemFile = new File(configuration.problemFilePath);
		if( !problemFile.exists()) {
			LOGGER.fatal("provided path to problem file not existing");
			return false;
		}

		numOfRandomWalkSteps = configuration.numOfRandomWalkSteps;
		numOfTracesToGenerate = configuration.numOfTracesToGenerate;

		sasFilePath = configuration.sasFilePath;
		tempDirPath = configuration.tempDirPath;
		tracesDirPath = configuration.tracesDirPath;

		pythonScriptsPath = configuration.pythonScriptsPath;

		return true;
	}

	@Override
	public void create() {

		LOGGER.info("create start:");

		convertToPDDL();

		LOGGER.info("parse agent file");
		addlParser = new ADDLParser().parse(agentsFile);

		translateToSAS();

		/* preprocessing the sas might remove some of the variables
		preprocessSAS();

		LOGGER.info("parse preprocessed sas file");

		sasParser = new SASParser(preprocesSASFile);

		preprocessor = new SASPreprocessor(sasParser.getDomain(), addlParser);
		 */

		LOGGER.info("parse sas file");

		sasParser = new SASParser(sasFile);

		preprocessor = new SASPreprocessor(sasParser.getDomain(), addlParser);

		createEntities(addlParser);

		runEntities();

		LOGGER.info("create end");
	}

	private void createEntities(ADDLObject addlParser) {
		LOGGER.info("create entities:");

		for (String agentName : addlParser.getAgentList()) {
			problems.add(preprocessor.getProblemForAgent(agentName));
		}
	}

	private void runEntities() {
		LOGGER.info("run entities:");

		SASGenerator sasGenerator = new SASGenerator();

		String TracesFolder = tracesDirPath + "/" + problemFileName;

		sasGenerator.generateFile(TracesFolder,problemFileName + "_Traces");

		// generate problems
		for (int i = 0; i < numOfTracesToGenerate; i++) {

			List<StateActionState> sasList = new ArrayList<StateActionState>();
			// perform random walk
			State endState = StateActionStateRandomWalker.RandomWalk(sasList, preprocessor.getGlobalInit(),
					preprocessor.getGlobalGoal(), numOfRandomWalkSteps, problems);

			String humenized = endState.getDomain().humanize(endState.getValues());

			newStateActionStates.put(humenized, sasList);
		}

		int problemCounter = 1;

		Iterator<Entry<String, List<StateActionState>>> it = newStateActionStates.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, List<StateActionState>> statePlanPair = (Entry<String, List<StateActionState>>)it.next();

			List<StateActionState> sasList = statePlanPair.getValue();

			// append new sasList
			sasGenerator.appendSASList(sasList, problemCounter);

			problemCounter++;

		}

		sasGenerator.close();

		delelteTemporaryFiles();
	}


	private void delelteTemporaryFiles() {

		LOGGER.info("Deleting temporary files");

		File temp = new File(tempDirPath);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'temp' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				System.exit(1);
			}
		}
	}

	private void convertToPDDL() {

		LOGGER.info("convert ma-pddl to pddl (ma-to-pddl.py)");

		String path = domainFile.getPath().substring(0, domainFile.getPath().lastIndexOf("/"));

		domainFileName = domainFile.getPath().substring(domainFile.getPath().lastIndexOf("/") + 1, domainFile.getPath().lastIndexOf("."));
		problemFileName = problemFile.getPath().substring(problemFile.getPath().lastIndexOf("/") + 1, problemFile.getPath().lastIndexOf("."));


		try {
			
			String scriptPath = pythonScriptsPath + "/" + CONVERTOR;

			String cmd = scriptPath + " " + path + " " + domainFileName + " " + problemFileName + " " + tempDirPath;
			
			LOGGER.info("RUN: " + cmd);
			
			ProcessBuilder pb = new ProcessBuilder(scriptPath, path, domainFileName, problemFileName, tempDirPath);
            pb.redirectOutput(Redirect.INHERIT);
            
            Process pr = pb.start();

			pr.waitFor();			
		} catch (Exception e) {
			LOGGER.fatal(e, e);
			System.exit(1);
		}

		LOGGER.info("set converted domain, problem and agent file paths");

		domainFilePath = tempDirPath + "/" + domainFileName + ".pddl";
		problemFilePath = tempDirPath + "/" + problemFileName + ".pddl";
		agentsFilePath = tempDirPath + "/" + problemFileName + ".addl";

		LOGGER.info("check the created agent file (.addl)");

		agentsFile = new File(agentsFilePath);
		if (!agentsFile.exists()) {
			LOGGER.fatal("Agent file " + agentsFilePath + " does not exist!");
			System.exit(1);
		}
	}

	private void translateToSAS() {

		LOGGER.info("translate pddl to .sas file (translate.py)");

		try {

			String scriptPath = pythonScriptsPath + "/" + TRANSLATOR;

			String cmd = scriptPath + " " + domainFilePath + " " + problemFilePath;
			
			LOGGER.info("RUN: " + cmd);
			
			ProcessBuilder pb = new ProcessBuilder(scriptPath, domainFilePath, problemFilePath);
            pb.redirectOutput(Redirect.INHERIT);
            
            Process pr = pb.start();

			pr.waitFor();			

		} catch (Exception e) {
			LOGGER.fatal(e, e);
			System.exit(1);
		}

		LOGGER.info("check the created .sas file (.sas)");

		sasFile = new File(sasFilePath);

		if (!sasFile.exists()) {
			LOGGER.fatal("SAS file " + sasFilePath + " does not exist!");
			System.exit(1);
		}
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
