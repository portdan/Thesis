package OurPlanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import Configuration.ConfigurationManager;
import Configuration.OurPlannerConfiguration;
import PlannerAndLearner.PlannerAndModelLearner;
import Utils.FileDeleter;
import Utils.VerificationResult;
import cz.agents.alite.creator.Creator;

public class OurPlanner implements Creator  {

	/* Global variables */
	private final static Logger LOGGER = Logger.getLogger(OurPlanner.class);
	private final static int ARGS_NUM = 2;
	private final static int SEED = 1;

	private static final String AGENT_PARSER_SCRIPT = "parse-agents.py";

	/* Class variables */
	private String configurationFilePath = "";

	private String domainFileName = "";
	private String problemFileName = "";

	public VerificationModel verificationModel = VerificationModel.GroundedModel;
	public PlanningModel planningModel = PlanningModel.SafeModel;
	
	private TraceLearner learner = null;

	private File tracesFile = null;
	private File outputTestFile = null;
	private File outputCopyDir = null;
	private File groundedFile = null;
	private File localViewFile = null;
	private File agentsFile = null;

	private List<String> agentList = null;
	private	String currentLeaderAgent = "";
	private List<String> availableLeaders= null;

	// uses seed so agent order is same
	private	Random rnd = new Random(SEED);

	private int numOftraces = 0;
	private int tracesLearinigInterval = 0;
	private int num_agents_solved = 0;
	private int num_agents_not_solved = 0;
	private int num_agents_timeout = 0;

	@Override
	public void create() {

		LOGGER.info("OurPlanner create");
	}

	@Override
	public void init(String[] args) {

		LOGGER.setLevel(Level.INFO);

		long startTime = System.currentTimeMillis();

		LOGGER.info("OurPlanner start");

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

		if(!ReadAgentsFile())
		{
			LOGGER.fatal("Agents file reading failure");
			System.exit(1);
		}

		TestDataAccumulator.startNewAccumulator(domainFileName, problemFileName, agentList);

		TestDataAccumulator.getAccumulator().setOutputFile(Globals.OUTPUT_TEST_FILE_PATH);


		if(!FileDeleter.deleteLearnedFiles()) {
			LOGGER.info("Deleting Learned files failure");
			System.exit(1);
		}

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			System.exit(1);
		}

		if(!copyOriginalProblem()) {
			LOGGER.info("Coping original files failure");
			System.exit(1);
		}
		/*
		if(!runPlanningAlgorithm())
		{
			LOGGER.fatal("Planning algorithem failure");
			System.exit(1);
		}
		 */

		//runPlanningAlgorithm();

		runPlanningAlgorithmWithModelUpdating();

		TestDataAccumulator.getAccumulator().numOfAgentsSolved = num_agents_solved;
		TestDataAccumulator.getAccumulator().numOfAgentsTimeout = num_agents_timeout;
		TestDataAccumulator.getAccumulator().numOfAgentsNotSolved = num_agents_not_solved;		

		if(!copyOutputFolder()) {
			LOGGER.info("Coping output folder failure");
			System.exit(1);
		}

		/*
		if(!deleteOutputFiles()) {
			LOGGER.info("Deleting Output files failure");
			System.exit(1);
		}
		 */

		long finishTime = System.currentTimeMillis();

		TestDataAccumulator.getAccumulator().totalTimeMs = finishTime-startTime;

		TestDataAccumulator.getAccumulator().writeOutput();

		LOGGER.info(TestDataAccumulator.getAccumulator().toString());
	}

	private boolean ApplyConfiguration(OurPlannerConfiguration configuration) {
		LOGGER.info("Applying configuration");

		File f = null;
		String extension = "";

		/*INPUT DIR PATH*/
		Globals.INPUT_PATH = configuration.inputDirPath;
		/*INPUT DIR PATH*/

		/*OUTPUT DIR PATH*/
		Globals.OUTPUT_PATH = configuration.outputDirPath;
		/*OUTPUT DIR PATH*/

		Globals.INPUT_GROUNDED_PATH = Globals.INPUT_PATH + "/" + configuration.inputGoundedDirName;
		groundedFile = new File(Globals.INPUT_GROUNDED_PATH);

		if( !groundedFile.exists()) {
			LOGGER.fatal("provided path to grounded folder not existing");
			return false;
		}

		Globals.INPUT_LOCAL_VIEW_PATH = Globals.INPUT_PATH + "/" + configuration.inputLocalViewDirName;
		localViewFile = new File(Globals.INPUT_LOCAL_VIEW_PATH);

		if( !localViewFile.exists()) {
			LOGGER.fatal("provided path to local view folder not existing");
			return false;
		}

		Globals.INPUT_TRACES_PATH = Globals.INPUT_PATH + "/" + configuration.inputTracesDirName;
		tracesFile = new File(Globals.INPUT_TRACES_PATH);

		if( !tracesFile.exists()) {
			LOGGER.fatal("provided path to traces folder not existing");
			return false;
		}

		Globals.INPUT_AGENTS_PATH = Globals.INPUT_PATH + "/" + configuration.inputAgentsDirName + "/" + configuration.agentsFileName;
		agentsFile = new File(Globals.INPUT_AGENTS_PATH);
		if( !agentsFile.exists()) {
			LOGGER.fatal("provided path to .agents file not existing");
			return false;
		}

		numOftraces = configuration.numOfTracesToUse;

		tracesLearinigInterval = configuration.tracesLearinigInterval;

		domainFileName = configuration.domainFileName;
		extension = domainFileName.substring(domainFileName.lastIndexOf(".") + 1);
		//Checks if input domain name is of .pddl type
		if (!extension.equals("pddl")) {
			LOGGER.fatal("provided domain file name not existing");
			return false;
		}

		problemFileName = configuration.problemFileName;
		extension = problemFileName.substring(problemFileName.lastIndexOf(".") + 1);
		//Checks if input problem name is of .pddl type
		if (!extension.equals("pddl")) {
			LOGGER.fatal("provided problem file name not existing");
			return false;
		}

		Globals.OUTPUT_COPY_PATH = configuration.outputCopyDirPath + "/" + numOftraces + "_traces";
		outputCopyDir = new File(Globals.OUTPUT_COPY_PATH);

		if( !outputCopyDir.exists()) {
			outputCopyDir.getParentFile().mkdirs();
		}

		Globals.OUTPUT_TEST_FILE_PATH = configuration.testOutputCSVFilePath;
		outputTestFile = new File(Globals.OUTPUT_TEST_FILE_PATH);

		if( !outputTestFile.exists()) {
			outputTestFile.getParentFile().mkdirs();
		}

		Globals.OUTPUT_TEMP_PATH = Globals.OUTPUT_PATH  + "/" + configuration.outputTempDirPath;

		Globals.OUTPUT_SAFE_MODEL_PATH = Globals.OUTPUT_PATH  + "/" + configuration.outputSafeModelLearningDirName;
		f = new File(Globals.OUTPUT_SAFE_MODEL_PATH);
		if( !f.exists()) {
			f.mkdirs();
		}

		Globals.OUTPUT_UNSAFE_MODEL_PATH = Globals.OUTPUT_PATH  + "/" + configuration.outputUnSafeModelLearningDirName;
		f = new File(Globals.OUTPUT_UNSAFE_MODEL_PATH);
		if( !f.exists()) {
			f.mkdirs();
		}

		Globals.OUTPUT_SOUND_MODEL_PATH = Globals.OUTPUT_PATH  + "/" + configuration.outputSoundModelLearningDirName;
		f = new File(Globals.OUTPUT_SOUND_MODEL_PATH);
		if( !f.exists()) {
			f.mkdirs();
		}

		Globals.SAS_OUTPUT_FILE_PATH = Globals.OUTPUT_PATH  + "/" + configuration.outputSASFileName;

		Globals.PROCESSED_SAS_OUTPUT_FILE_PATH = Globals.OUTPUT_PATH  + "/" + configuration.outputSASFileName.split("\\.")[0];

		Globals.PYTHON_SCRIPTS_FOLDER = configuration.pythonScriptsPath;

		verificationModel = configuration.verificationModel;

		planningModel = configuration.planningModel;

		return true;
	}

	private boolean runPlanningAlgorithm() {

		LOGGER.info("Running planning algorithm");

		availableLeaders = new ArrayList<>(agentList);

		List<String> leaderAgentPlan = null;

		int rounds = 1;

		long learningStartTime = System.currentTimeMillis();

		boolean isLearning = false;

		if(numOftraces>0)
			isLearning = learnSafeAndUnSafeModelsFromTraces();

		long learningFinishTime = System.currentTimeMillis();

		TestDataAccumulator.getAccumulator().totalLearningTimeMs = learningFinishTime - learningStartTime;

		while(leaderAgentPlan == null) {

			LOGGER.info("Round " + rounds + " - Start!");
			rounds++;

			if(availableLeaders.isEmpty()){

				LOGGER.fatal("No more available leaders. No solution!");
				break;
			}

			currentLeaderAgent = pickLeader();

			//			TestDataAccumulator.getAccumulator().trainingSize = 0;
			//
			//			long learningStartTime = System.currentTimeMillis();
			//
			//			boolean isLearning = learnFromTraces(currentLeaderAgent);
			//
			//			long learningFinishTime = System.currentTimeMillis();
			//
			//			TestDataAccumulator.getAccumulator().totalLearningTimeMs += learningFinishTime - learningStartTime;
			//			TestDataAccumulator.getAccumulator().agentLearningTimeMs.put(currentLeaderAgent, learningFinishTime - learningStartTime);


			LOGGER.info("Current Leader Agent " + currentLeaderAgent);

			long planningStartTime = System.currentTimeMillis();

			leaderAgentPlan = planForAgent(currentLeaderAgent, isLearning, planningModel);

			long planningFinishTime = System.currentTimeMillis();

			TestDataAccumulator.getAccumulator().totalPlaningTimeMs += planningFinishTime - planningStartTime;
			TestDataAccumulator.getAccumulator().agentPlanningTimeMs.put(currentLeaderAgent, planningFinishTime - planningStartTime);

			if(leaderAgentPlan == null)
				continue;		

			if(verifyPlan(leaderAgentPlan, isLearning, verificationModel)) {

				TestDataAccumulator.getAccumulator().solvingAgent = currentLeaderAgent;
				TestDataAccumulator.getAccumulator().planLength= leaderAgentPlan.size();

				return true;
			}
		}

		return false;
	}

	private boolean runPlanningAlgorithmWithModelUpdating() {

		LOGGER.info("Running planning algorithm with model updating");

		availableLeaders = new ArrayList<>(agentList);

		List<String> leaderAgentPlan = null;

		long learningStartTime = System.currentTimeMillis();

		if(numOftraces>0)
			learnSafeAndUnSafeModelsFromTraces();

		long learningFinishTime = System.currentTimeMillis();

		TestDataAccumulator.getAccumulator().totalLearningTimeMs = learningFinishTime - learningStartTime;

		while(!availableLeaders.isEmpty()) {

			currentLeaderAgent = pickLeader();

			LOGGER.info("Current Leader Agent " + currentLeaderAgent);

			long planningStartTime = System.currentTimeMillis();

			leaderAgentPlan = planAndLearnModels(currentLeaderAgent);

			long planningFinishTime = System.currentTimeMillis();

			TestDataAccumulator.getAccumulator().totalPlaningTimeMs += planningFinishTime - planningStartTime;
			TestDataAccumulator.getAccumulator().agentPlanningTimeMs.put(currentLeaderAgent, planningFinishTime - planningStartTime);

			if(leaderAgentPlan != null)
				return true;
		}

		LOGGER.fatal("No more available leaders. No solution!");

		return false;
	}

	private List<String> planAndLearnModels(String agentName) {

		PlannerAndModelLearner asd = new PlannerAndModelLearner(agentName, agentList,
				domainFileName, problemFileName, learner);
		
		asd.test();

		return null;
	}

	private boolean learnSafeAndUnSafeModelsFromTraces() {

		LOGGER.info("Running learning algorithm");

		learner = new TraceLearner(agentList,tracesFile, 
				groundedFile, localViewFile, domainFileName, problemFileName, numOftraces, tracesLearinigInterval);	

		boolean isLearned = learner.learnNewActions();

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return false;
		}

		return isLearned;

	}

	private boolean copyOriginalProblem() {

		LOGGER.info("Copy the original problem files");

		File destDir = new File(Globals.OUTPUT_SAFE_MODEL_PATH);

		try {
			FileUtils.copyDirectory(localViewFile, destDir);
		} catch (IOException e) {
			LOGGER.fatal(e, e);
			return false;
		}

		destDir = new File(Globals.OUTPUT_UNSAFE_MODEL_PATH);

		try {
			FileUtils.copyDirectory(localViewFile, destDir);
		} catch (IOException e) {
			LOGGER.fatal(e, e);
			return false;
		}

		return true;
	}

	private boolean copyOutputFolder() {

		LOGGER.info("Copy the output files");

		File srcDir = new File(Globals.OUTPUT_PATH);

		try {
			FileUtils.copyDirectory(srcDir, outputCopyDir);
		} catch (IOException e) {
			LOGGER.fatal(e, e);
			return false;
		}

		return true;
	}


	private boolean verifyPlan(List<String> plan, boolean isLearning, VerificationModel model) {

		LOGGER.info("Verifing plan");

		String problemFilesPath = "";

		boolean useGrounded = false;

		if (isLearning) {

			switch (model) {
			case SafeModel:
				problemFilesPath = Globals.OUTPUT_SAFE_MODEL_PATH;
				break;
			case UnSafeModel:
				problemFilesPath = Globals.OUTPUT_UNSAFE_MODEL_PATH;
				break;
			case GroundedModel:
			default:
				problemFilesPath = Globals.INPUT_GROUNDED_PATH;
				useGrounded = true;
				break;
			}
		}else {
			problemFilesPath = Globals.INPUT_LOCAL_VIEW_PATH;
		}

		PlanVerifier planVerifier = new PlanVerifier(agentList,domainFileName,problemFileName,
				problemFilesPath, useGrounded);		

		VerificationResult res = planVerifier.verifyPlan(plan,0);

		if(res.isVerified)
			num_agents_solved++;

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return false;	
		}

		return res.isVerified;
	}

	private List<String> planForAgent(String agentName, boolean isLearning, PlanningModel model ) {

		LOGGER.info("Planning for leader agent");

		String agentDomainPath = "";

		if (isLearning) {

			switch (model) {
			case UnSafeModel:
				agentDomainPath = Globals.OUTPUT_UNSAFE_MODEL_PATH  + "/" + agentName + "/" + domainFileName;
				break;
			case SafeModel:
				agentDomainPath = Globals.OUTPUT_SAFE_MODEL_PATH  + "/" + agentName + "/" + domainFileName;
				break;
			case GroundedModel:
			default:
				agentDomainPath = Globals.INPUT_GROUNDED_PATH + "/" + domainFileName;
				break;
			}
		}
		else 
			agentDomainPath = Globals.INPUT_LOCAL_VIEW_PATH + "/" + agentName + "/" + domainFileName;

		String agentProblemPath = Globals.INPUT_LOCAL_VIEW_PATH + "/" + agentName + "/" + problemFileName;
		String agentADDLPath = Globals.OUTPUT_TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";
		String heuristic = "saFF-glcl";
		int recursionLevel = -1;
		double timeLimitMin = 1;

		//agentDomainPath = groundedFolder + "/" + domainFileName;

		MADLAPlanner planner = new MADLAPlanner(agentDomainPath, agentProblemPath, agentADDLPath,
				heuristic, recursionLevel, timeLimitMin, agentList, agentName);

		List<String> result = planner.plan();

		if(planner.isTimeout)
			num_agents_timeout++;

		if(planner.isNotSolved)
			num_agents_not_solved++;

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;
		}

		return result;
	}

	private String pickLeader() {

		LOGGER.info("Picking leader agent");

		String leaderAgent;
		int leaderAgentIndex = rnd.nextInt(availableLeaders.size());
		leaderAgent = availableLeaders.get(leaderAgentIndex);

		availableLeaders.remove(leaderAgentIndex);

		LOGGER.info("Leader agent: "+ leaderAgent);

		return leaderAgent;
	}

	private boolean ReadAgentsFile() {

		LOGGER.info("Agents file reading");

		ExecCommand ec = null;

		try {

			String domainName = domainFileName.substring(0,domainFileName.lastIndexOf(".") );
			String problemName = problemFileName.substring(0,problemFileName.lastIndexOf(".") );

			String scriptPath = Globals.PYTHON_SCRIPTS_FOLDER + "/" + AGENT_PARSER_SCRIPT;

			String cmd = scriptPath + " " + Globals.INPUT_AGENTS_PATH + " " + domainName + " " + problemName;

			LOGGER.info("Running: " + cmd);

			//			pr = Runtime.getRuntime().exec(cmd);
			//			pr.waitFor();

			ec = new ExecCommand(cmd);
		}
		catch (Exception e) {
			LOGGER.info(e,e);
			return false;
		}


		//		} 
		//		catch (Exception e) {
		//			LOGGER.fatal(e, e);
		//			return false;
		//		}

		return agentsReaderValid(ec);
	}

	private boolean agentsReaderValid(ExecCommand ec) {

		LOGGER.info("Agents reading check");

		if(ec == null) {
			LOGGER.fatal("agent-parser.py script failure");
			return false;
		}

		List<String> output = new ArrayList<String>();

		String[] split = ec.getOutput().split("\n");

		for (int i = 0; i < split.length; i++) {
			output.add(split[i]);                                                                                       
		}

		agentList = new ArrayList<>(output);

		Collections.sort(agentList);

		LOGGER.info("Agents are: " + agentList);

		return true;
	}

	private boolean ParseArgs(String[] args) {

		LOGGER.info("Args validity check");

		String status = "";
		boolean valid = true;

		if ((valid = ArgsLenghtValid(args)) == false) 
			status = "Usage: <grounded folder> <localview folder> <traces folder> <domain name> <problem name> <addl name>";	

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
