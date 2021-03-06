package OurPlanner;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.sun.management.OperatingSystemMXBean;

import Configuration.ConfigurationManager;
import Configuration.OurPlannerConfiguration;
import PlannerAndLearner.LikelyModelStatus;
import PlannerAndLearner.PlannerAndLikelyModelLearner;
import PlannerAndLearner.PlannerAndModelLearner;
import Utils.FileDeleter;
import Utils.OSStatistics;
import Utils.TestDataAccumulator;
import Utils.VerificationResult;
import cz.agents.alite.creator.Creator;
import enums.IterationMethod;
import enums.PlannerMode;
import enums.PlanningModel;
import enums.VerificationModel;

public class OurPlanner implements Creator {

	/* Global variables */
	private final static Logger LOGGER = Logger.getLogger(OurPlanner.class);

	private final static int ARGS_NUM = 2;
	private final static int SEED = 1;

	private static final String AGENT_PARSER_SCRIPT = "parse-agents.py";

	/* Class variables */
	private String configurationFilePath = "";
	private String domainName = "";
	private String domainFileName = "";
	private String problemFileName = "";
	private String configFilePath = "";

	public VerificationModel verificationModel = VerificationModel.GroundedModel;
	public PlanningModel planningModel = PlanningModel.SafeModel;
	public PlannerMode plannerMode = PlannerMode.Planning;
	public IterationMethod iterationMethod = IterationMethod.Random;

	private TraceLearner learner = null;

	private int timeoutInMS = 0;
	private int cValue = 2;

	private File tracesFile = null;
	private File outputTestFile = null;
	private File outputCopyDir = null;
	private File groundedFile = null;
	private File localViewFile = null;
	private File agentsFile = null;

	private List<String> agentList = null;
	private String currentLeaderAgent = "";
	private String experimentDetails = "";
	private List<String> availableLeaders = null;

	// uses seed so agent order is same
	private Random rnd = new Random(SEED);
	// private Random rnd = new Random();

	private int numOftracesToUse = 0;
	private double tracesBucket = 0;
	private int tracesLearinigInterval = 0;
	private int num_agents_solved = 0;
	private int num_agents_not_solved = 0;
	private int num_agents_timeout = 0;

	private long startTimeMs = 0;

	private int planningTimeoutInMS = 10000;

	OperatingSystemMXBean OSstatistics = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

	@Override
	public void create() {

		LOGGER.info("OurPlanner create");
	}

	@Override
	public void init(String[] args) {

		LOGGER.setLevel(Level.INFO);

		startTimeMs = System.currentTimeMillis();

		LOGGER.info("OurPlanner start");

		if (!ParseArgs(args)) {
			LOGGER.fatal("Args not valid");
			System.exit(1);
		}

		if (!ConfigurationManager.getInstance().loadConfiguration(configurationFilePath)) {
			LOGGER.fatal("Configuration loading failure");
			System.exit(1);
		}

		if (!ApplyConfiguration(ConfigurationManager.getInstance().getCurrentConfiguration())) {
			LOGGER.fatal("Configuration setting failure");
			System.exit(1);
		}

		if (!ReadAgentsFile()) {
			LOGGER.fatal("Agents file reading failure");
			System.exit(1);
		}

		TestDataAccumulator.startNewAccumulator(domainName, problemFileName, agentList);

		TestDataAccumulator.getAccumulator().setOutputFile(Globals.OUTPUT_TEST_FILE_PATH);

		if (!FileDeleter.deleteLearnedFiles()) {
			LOGGER.info("Deleting Learned files failure");
			System.exit(1);
		}

		if (!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			System.exit(1);
		}

		if (!copyOriginalProblem()) {
			LOGGER.info("Coping original files failure");
			System.exit(1);
		}

		TestDataAccumulator.getAccumulator().experimentDetails = experimentDetails;
		TestDataAccumulator.getAccumulator().method = iterationMethod.name();
		TestDataAccumulator.getAccumulator().initialTrainingSize = numOftracesToUse;
		TestDataAccumulator.getAccumulator().trainingSizeBucket = tracesBucket;

		/*
		 * try { FileUtils.copyDirectory(tracesFile, outputCopyDir); } catch
		 * (IOException e) { // TODO Auto-generated catch block e.printStackTrace(); }
		 */

		if (plannerMode == PlannerMode.Planning) {

			runPlanningAlgorithm();

			TestDataAccumulator.getAccumulator().numOfAgentsSolved = num_agents_solved;
			TestDataAccumulator.getAccumulator().numOfAgentsTimeout = num_agents_timeout;
			TestDataAccumulator.getAccumulator().numOfAgentsNotSolved = num_agents_not_solved;

		} else if (plannerMode == PlannerMode.PlanningAndLearning)
			runPlanningAlgorithmWithModelUpdating();
		else if (plannerMode == PlannerMode.PlanningAndLearningLikely)
			runPlanningAlgorithmWithLikelyModelUpdating();

		if (!copyOutputFolder()) {
			LOGGER.info("Coping output folder failure");
			// System.exit(1);
		}

		/*
		 * if(!deleteOutputFiles()) { LOGGER.info("Deleting Output files failure");
		 * System.exit(1); }
		 */

		long finishTimeMs = System.currentTimeMillis();

		TestDataAccumulator.getAccumulator().totalTimeMs = finishTimeMs - startTimeMs;

		TestDataAccumulator.getAccumulator().writeOutput();

		LOGGER.info(TestDataAccumulator.getAccumulator().toString());
	}

	private boolean ApplyConfiguration(OurPlannerConfiguration configuration) {
		LOGGER.info("Applying configuration");

		File f = null;
		String extension = "";

		verificationModel = configuration.verificationModel;

		planningModel = configuration.planningModel;

		plannerMode = configuration.plannerMode;

		iterationMethod = configuration.iterationMethod;

		timeoutInMS = configuration.timeoutInMS;

		cValue = configuration.cValue;

		experimentDetails = configuration.experimentDetails;

		planningTimeoutInMS = configuration.planningTimeoutInMS;

		/* INPUT DIR PATH */
		Globals.INPUT_PATH = configuration.inputDirPath;
		/* INPUT DIR PATH */

		/* OUTPUT DIR PATH */
		Globals.OUTPUT_PATH = configuration.outputDirPath;
		/* OUTPUT DIR PATH */

		Globals.INPUT_GROUNDED_PATH = Globals.INPUT_PATH + "/" + configuration.inputGoundedDirName;
		groundedFile = new File(Globals.INPUT_GROUNDED_PATH);

		if (!groundedFile.exists()) {
			LOGGER.fatal("provided path to grounded folder not existing");
			return false;
		}

		Globals.INPUT_LOCAL_VIEW_PATH = Globals.INPUT_PATH + "/" + configuration.inputLocalViewDirName;
		localViewFile = new File(Globals.INPUT_LOCAL_VIEW_PATH);

		if (!localViewFile.exists()) {
			LOGGER.fatal("provided path to local view folder not existing");
			return false;
		}

		Globals.INPUT_TRACES_PATH = Globals.INPUT_PATH + "/" + configuration.inputTracesDirName;
		tracesFile = new File(Globals.INPUT_TRACES_PATH);

		if (!tracesFile.exists()) {
			LOGGER.fatal("provided path to traces folder not existing");
			return false;
		}

		Globals.INPUT_AGENTS_PATH = Globals.INPUT_PATH + "/" + configuration.inputAgentsDirName + "/"
				+ configuration.agentsFileName;
		agentsFile = new File(Globals.INPUT_AGENTS_PATH);
		if (!agentsFile.exists()) {
			LOGGER.fatal("provided path to .agents file not existing");
			return false;
		}

		numOftracesToUse = configuration.numOfTracesToUse;
		tracesBucket = configuration.totalTracesBucket;

		tracesLearinigInterval = configuration.tracesLearinigInterval;

		domainFileName = configuration.domainFileName;
		domainName = configuration.domainName;

		extension = domainFileName.substring(domainFileName.lastIndexOf(".") + 1);
		// Checks if input domain name is of .pddl type
		if (!extension.equals("pddl")) {
			LOGGER.fatal("provided domain file name not existing");
			return false;
		}

		Globals.LIKELY_MODEL_GENERATOR_PATH = configuration.likelyModelGeneratorPath;
		configFilePath = configuration.configFilePath;

		problemFileName = configuration.problemFileName;
		extension = problemFileName.substring(problemFileName.lastIndexOf(".") + 1);
		// Checks if input problem name is of .pddl type
		if (!extension.equals("pddl")) {
			LOGGER.fatal("provided problem file name not existing");
			return false;
		}

		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		String timestampFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(timestamp);

		Globals.OUTPUT_COPY_PATH = configuration.outputCopyDirPath + "/" + experimentDetails + "/"
				+ iterationMethod.name() + "/" + numOftracesToUse + "_traces" + "/" + timestampFormat;

		outputCopyDir = new File(Globals.OUTPUT_COPY_PATH);

		if (!outputCopyDir.exists()) {
			outputCopyDir.getParentFile().mkdirs();
		}

		Globals.OUTPUT_TEST_FILE_PATH = configuration.testOutputCSVFilePath;
		outputTestFile = new File(Globals.OUTPUT_TEST_FILE_PATH);

		if (!outputTestFile.exists()) {
			outputTestFile.getParentFile().mkdirs();
		}

		Globals.OUTPUT_TEMP_PATH = Globals.OUTPUT_PATH + "/" + configuration.outputTempDirPath;

		Globals.OUTPUT_SAFE_MODEL_PATH = Globals.OUTPUT_PATH + "/" + configuration.outputSafeModelLearningDirName;
		f = new File(Globals.OUTPUT_SAFE_MODEL_PATH);
		if (!f.exists()) {
			f.mkdirs();
		}

		Globals.OUTPUT_UNSAFE_MODEL_PATH = Globals.OUTPUT_PATH + "/" + configuration.outputUnSafeModelLearningDirName;
		f = new File(Globals.OUTPUT_UNSAFE_MODEL_PATH);
		if (!f.exists()) {
			f.mkdirs();
		}

		Globals.OUTPUT_SOUND_MODEL_PATH = Globals.OUTPUT_PATH + "/" + configuration.outputSoundModelLearningDirName;
		f = new File(Globals.OUTPUT_SOUND_MODEL_PATH);
		if (!f.exists()) {
			f.mkdirs();
		}

		Globals.SAS_OUTPUT_FILE_PATH = Globals.OUTPUT_PATH + "/" + configuration.outputSASFileName;

		Globals.PROCESSED_SAS_OUTPUT_FILE_PATH = Globals.OUTPUT_PATH + "/"
				+ configuration.outputSASFileName.split("\\.")[0];

		Globals.PYTHON_SCRIPTS_FOLDER = configuration.pythonScriptsPath;

		return true;
	}

	private boolean runPlanningAlgorithm() {

		LOGGER.info("Running planning algorithm");

		availableLeaders = new ArrayList<>(agentList);
		long passedTimeMS = 0;

		long agentTimeoutInMS = timeoutInMS / agentList.size();

		List<String> leaderAgentPlan = null;

		boolean isLearning = false;
		boolean isTimeout = false;

		if (numOftracesToUse >= 0)
			isLearning = learnSafeAndUnSafeModelsFromTraces();

		LogLearningTimes();

		if (Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT)
			timeoutInMS += TestDataAccumulator.getAccumulator().totalLearningTimeMs;

		LOGGER.info("Garbage collection!");
		System.gc();

		while (leaderAgentPlan == null) {

			passedTimeMS = System.currentTimeMillis() - startTimeMs;
			/*
			 * if(!Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT) passedTimeMS =
			 * TestDataAccumulator.getAccumulator().getTotalTimeMSWithoutOfflienLearning();
			 * else passedTimeMS = TestDataAccumulator.getAccumulator().getTotalTimeMS();
			 */

			if (passedTimeMS > timeoutInMS) {

				isTimeout = true;
				LOGGER.fatal("TIMEOUT!");
				break;
			}

			/*
			 * double getCommittedVirtualMemorySize =
			 * (double)OSstatistics.getCommittedVirtualMemorySize()/1073741824; double
			 * getFreePhysicalMemorySize =
			 * (double)OSstatistics.getFreePhysicalMemorySize()/1073741824; double
			 * getTotalPhysicalMemorySize =
			 * (double)OSstatistics.getTotalPhysicalMemorySize()/1073741824;
			 * 
			 * double usedMemoryRatio =
			 * (double)(getTotalPhysicalMemorySize-getFreePhysicalMemorySize)/(double)
			 * getTotalPhysicalMemorySize;
			 */
			double usedMemoryRatio = OSStatistics.GetMemoryUsage();

			if (usedMemoryRatio > Globals.MEMORY_OVER_USAGE_RATIO) {

				isTimeout = true;
				LOGGER.fatal("MEMORY OVER USAGE!");
				break;
			}

			if (availableLeaders.isEmpty()) {

				LOGGER.fatal("No more available leaders. No solution!");
				break;
			}

			currentLeaderAgent = pickLeader();

			TestDataAccumulator.getAccumulator().agentNumOfIterations.put(currentLeaderAgent, 1);

			LOGGER.info("Current Leader Agent " + currentLeaderAgent);

			long agentLearningTime = 0;

			if (Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT)
				agentLearningTime = TestDataAccumulator.getAccumulator().agentOfflineLearningTimeMs
				.get(currentLeaderAgent);

			long planningTimeoutMS = agentTimeoutInMS;// + agentLearningTime;

			long planningStartTime = System.currentTimeMillis();

			leaderAgentPlan = planForAgent(currentLeaderAgent, isLearning, planningModel, planningTimeoutMS);

			long planningTimeTotal = System.currentTimeMillis() - planningStartTime;

			LOGGER.info("Garbage collection!");
			System.gc();

			TestDataAccumulator.getAccumulator().totalPlaningTimeMs += planningTimeTotal;
			TestDataAccumulator.getAccumulator().agentPlanningTimeMs.put(currentLeaderAgent, planningTimeTotal);

			if (leaderAgentPlan == null)
				continue;

			long verifingTimeoutMS = agentTimeoutInMS - planningTimeTotal + agentLearningTime;

			long verifingStartTime = System.currentTimeMillis();

			boolean isVerified = verifyPlan(leaderAgentPlan, isLearning, verificationModel, verifingTimeoutMS);

			long verifingTotalTime = System.currentTimeMillis() - verifingStartTime;

			TestDataAccumulator.getAccumulator().totalVerifingTimeMs += verifingTotalTime;
			TestDataAccumulator.getAccumulator().agentVerifingTimeMs.put(currentLeaderAgent, verifingTotalTime);

			LOGGER.info("Garbage collection!");
			System.gc();

			if (isVerified) {

				TestDataAccumulator.getAccumulator().finishStatus = "SOLVED";
				TestDataAccumulator.getAccumulator().solvingAgent = currentLeaderAgent;
				TestDataAccumulator.getAccumulator().planLength = leaderAgentPlan.size();

				return true;
			} else
				leaderAgentPlan = null;
		}

		if (isTimeout || num_agents_timeout == agentList.size())
			TestDataAccumulator.getAccumulator().finishStatus = "TIMEOUT";
		else
			TestDataAccumulator.getAccumulator().finishStatus = "NOT SOLVED";

		return false;
	}

	private boolean runPlanningAlgorithmWithModelUpdating() {

		LOGGER.info("Running planning algorithm with model updating");

		availableLeaders = new ArrayList<>(agentList);

		List<String> leaderAgentPlan = null;
		long passedTimeMS = 0;

		boolean isTimeout = false;

		double timeLimitForAgent = ((double) timeoutInMS) / agentList.size();

		if (numOftracesToUse >= 0)
			learnSafeAndUnSafeModelsFromTraces();

		LOGGER.info("Garbage collection!");
		System.gc();

		LogLearningTimes();

		if (Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT)
			timeoutInMS += TestDataAccumulator.getAccumulator().totalLearningTimeMs;

		while (!availableLeaders.isEmpty()) {

			passedTimeMS = System.currentTimeMillis() - startTimeMs;
			/*
			 * if(!Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT) passedTimeMS =
			 * TestDataAccumulator.getAccumulator().getTotalTimeMSWithoutOfflienLearning();
			 * else passedTimeMS = TestDataAccumulator.getAccumulator().getTotalTimeMS();
			 */
			if (passedTimeMS > timeoutInMS) {
				isTimeout = true;
				LOGGER.fatal("TIMEOUT!");
				break;
			}

			currentLeaderAgent = pickLeader();

			LOGGER.info("Current Leader Agent " + currentLeaderAgent);

			long agentLearningTime = 0;

			if (Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT)
				agentLearningTime = TestDataAccumulator.getAccumulator().agentOfflineLearningTimeMs
				.get(currentLeaderAgent);

			double timeoutMS = timeLimitForAgent + agentLearningTime;

			PlannerAndModelLearner plannerAndLearner = new PlannerAndModelLearner(currentLeaderAgent, agentList,
					domainFileName, problemFileName, learner, learner.getGoalFacts(), System.currentTimeMillis(),
					timeoutMS, planningTimeoutInMS);

			if (iterationMethod == IterationMethod.Monte_Carlo_Reliability_Heuristic) {

				TestDataAccumulator.getAccumulator().method = "Monte_Carlo_Reliability_Heuristic C value = " + cValue;
				leaderAgentPlan = plannerAndLearner.planAndLearnMonteCarlo(IterationMethod.Reliability_Heuristic,
						cValue);
			} else if (iterationMethod == IterationMethod.Monte_Carlo_Goal_Proximity_Heuristic) {

				TestDataAccumulator.getAccumulator().method = "Monte_Carlo_Goal_Proximity_Heuristic C value = "
						+ cValue;
				leaderAgentPlan = plannerAndLearner.planAndLearnMonteCarlo(IterationMethod.Goal_Proximity_Heuristic,
						cValue);
			} else if (iterationMethod == IterationMethod.Monte_Carlo_Plan_Length_Heuristic) {

				TestDataAccumulator.getAccumulator().method = "Monte_Carlo_Plan_Length_Heuristic C value = " + cValue;
				leaderAgentPlan = plannerAndLearner.planAndLearnMonteCarlo(IterationMethod.Plan_Length_Heuristic,
						cValue);
			} else {
				leaderAgentPlan = plannerAndLearner.planAndLearn(iterationMethod);
			}

			TestDataAccumulator.getAccumulator().numOfAgentsSolved += plannerAndLearner.num_agents_solved;
			TestDataAccumulator.getAccumulator().numOfAgentsTimeout += plannerAndLearner.num_agents_timeout;
			TestDataAccumulator.getAccumulator().numOfAgentsNotSolved += plannerAndLearner.num_agents_not_solved;

			TestDataAccumulator.getAccumulator().agentNumOfIterations.put(currentLeaderAgent,
					plannerAndLearner.numOfIterations);
			TestDataAccumulator.getAccumulator().agentAddedTrainingSize.put(currentLeaderAgent,
					plannerAndLearner.addedTrainingSize);

			LogLearningTimes();

			LOGGER.info("Garbage collection!");
			System.gc();

			if (leaderAgentPlan != null) {

				TestDataAccumulator.getAccumulator().finishStatus = "SOLVED";
				TestDataAccumulator.getAccumulator().solvingAgent = currentLeaderAgent;
				TestDataAccumulator.getAccumulator().planLength = leaderAgentPlan.size();

				return true;
			}

			// if(plannerAndLearner.isTimeout)
			// {
			// isTimeout=true;
			// LOGGER.fatal("TIMEOUT!");
			// break;
			// }
		}

		if (isTimeout)
			TestDataAccumulator.getAccumulator().finishStatus = "TIMEOUT";
		else
			TestDataAccumulator.getAccumulator().finishStatus = "NOT SOLVED";

		return false;
	}

	private boolean runPlanningAlgorithmWithLikelyModelUpdating() {

		LOGGER.info("Running planning algorithm with likely model updating");

		availableLeaders = new ArrayList<>(agentList);

		List<String> leaderAgentPlan = null;
		long passedTimeMS = 0;

		boolean isTimeout = false;

		double timeLimitForAgent = ((double) timeoutInMS) / agentList.size();

		DeleteEffectGenerator DEGenerator = new DeleteEffectGenerator (groundedFile, domainFileName, problemFileName);

		Set<String> actionNames = DEGenerator.generateAllActionLabels();
		Set<String> factNames = DEGenerator.generateAllFacts();

		Map<String, Set<String>> variableToValuesMapping = getVariableToValuesMapping(false);

		if (!PlannerAndLikelyModelLearner.setupLikelyModelGenerator(tracesFile, agentList, actionNames, variableToValuesMapping))			
			return false;

		availableLeaders = new ArrayList<>(agentList);

		while (!availableLeaders.isEmpty()) {

			passedTimeMS = System.currentTimeMillis() - startTimeMs;
			/*
			 * if(!Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT) passedTimeMS =
			 * TestDataAccumulator.getAccumulator().getTotalTimeMSWithoutOfflienLearning();
			 * else passedTimeMS = TestDataAccumulator.getAccumulator().getTotalTimeMS();
			 */
			if (passedTimeMS > timeoutInMS) {
				isTimeout = true;
				LOGGER.fatal("TIMEOUT!");
				break;
			}

			if (!PlannerAndLikelyModelLearner.clearLikelyModelGeneratorFiles())
				return false;

			currentLeaderAgent = pickLeader();

			long learningStartTime = System.currentTimeMillis();

			if (!PlannerAndLikelyModelLearner.preprocessData(configFilePath))
				return false;

			long totalLeariningTime = System.currentTimeMillis() - learningStartTime;

			LOGGER.info("Garbage collection!");
			System.gc();

			LikelyModelStatus status = PlannerAndLikelyModelLearner.readLikelyModelStatus(agentList);
			TestDataAccumulator.getAccumulator().agentAllActionsPresent = status.getAgentActionsPresesnt();

			TestDataAccumulator.getAccumulator().agentOfflineLearningTimeMs.put(currentLeaderAgent,totalLeariningTime);

			double timeoutMS = timeLimitForAgent + totalLeariningTime;

			PlannerAndLikelyModelLearner plannerAndLikelyModelLearner =
					new PlannerAndLikelyModelLearner(groundedFile, localViewFile, domainFileName, problemFileName,configFilePath,
							currentLeaderAgent,agentList, System.currentTimeMillis(),timeoutMS, planningTimeoutInMS);

			leaderAgentPlan = plannerAndLikelyModelLearner.plan();

			LOGGER.info("Garbage collection!");
			System.gc();

			TestDataAccumulator.getAccumulator().agentLearningTimeMs.put(currentLeaderAgent,
					totalLeariningTime+plannerAndLikelyModelLearner.agentLearningTime);

			TestDataAccumulator.getAccumulator().numOfAgentsSolved += plannerAndLikelyModelLearner.num_agents_solved;
			TestDataAccumulator.getAccumulator().numOfAgentsTimeout += plannerAndLikelyModelLearner.num_agents_timeout;
			TestDataAccumulator.getAccumulator().numOfAgentsNotSolved += plannerAndLikelyModelLearner.num_agents_not_solved;

			TestDataAccumulator.getAccumulator().agentNumOfIterations.put(currentLeaderAgent,
					plannerAndLikelyModelLearner.num_of_iterations);
			TestDataAccumulator.getAccumulator().agentAddedTrainingSize.put(currentLeaderAgent,
					plannerAndLikelyModelLearner.num_of_added_traces);

			if (leaderAgentPlan != null) {

				TestDataAccumulator.getAccumulator().finishStatus = "SOLVED";
				TestDataAccumulator.getAccumulator().solvingAgent = currentLeaderAgent;
				TestDataAccumulator.getAccumulator().planLength = leaderAgentPlan.size();

				return true;
			}
		}

		if (isTimeout)
			TestDataAccumulator.getAccumulator().finishStatus = "TIMEOUT";
		else
			TestDataAccumulator.getAccumulator().finishStatus = "NOT SOLVED";

		return false;
	}


	private void LogLearningTimes() {
		for (String agentName : agentList) {

			long agentLearningTime = 0;

			for (String otherAgentName : agentList)
				if (!otherAgentName.equals(agentName))
					agentLearningTime += learner.agentLearningTimes.get(otherAgentName);

			// TestDataAccumulator.getAccumulator().agentLearningTimeMs.put(agentName,
			// agentLearningTime);
			TestDataAccumulator.getAccumulator().agentLearningTimeMs.put(agentName,
					learner.agentLearningTimes.get(agentName));
			TestDataAccumulator.getAccumulator().agentOfflineLearningTimeMs.put(agentName,
					learner.agentLearningTimes.get(agentName));
		}
	}

	private boolean learnSafeAndUnSafeModelsFromTraces() {

		LOGGER.info("Running learning algorithm");

		learner = new TraceLearner(agentList, tracesFile, groundedFile, localViewFile, domainFileName, problemFileName,
				numOftracesToUse, tracesLearinigInterval, startTimeMs, timeoutInMS,
				Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT);

		boolean isLearned = learner.learnSafeAndUnSafeModels();

		if (!FileDeleter.deleteTempFiles()) {
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

	private boolean verifyPlan(List<String> plan, boolean isLearning, VerificationModel model, long timeoutMS) {

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
		} else {
			problemFilesPath = Globals.INPUT_LOCAL_VIEW_PATH;
		}

		PlanVerifier planVerifier = new PlanVerifier(agentList, domainFileName, problemFileName, timeoutMS,
				problemFilesPath, useGrounded);

		VerificationResult res = planVerifier.verifyPlan(plan, 0);

		if (!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return false;
		}

		if (res != null) {
			if (res.isVerified) {
				num_agents_solved++;
				return true;
			} else if (res.isTimeout) {
				num_agents_timeout++;
				return false;
			} else
				return false;
		} else
			return false;
	}

	private List<String> planForAgent(String agentName, boolean isLearning, PlanningModel model, long timeoutMS) {

		LOGGER.info("Planning for leader agent");

		String agentDomainPath = "";

		if (isLearning) {

			switch (model) {
			case UnSafeModel:
				agentDomainPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
				break;
			case SafeModel:
				agentDomainPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
				break;
			case GroundedModel:
			default:
				agentDomainPath = Globals.INPUT_GROUNDED_PATH + "/" + domainFileName;
				break;
			}
		} else
			agentDomainPath = Globals.INPUT_LOCAL_VIEW_PATH + "/" + agentName + "/" + domainFileName;

		String agentProblemPath = Globals.INPUT_LOCAL_VIEW_PATH + "/" + agentName + "/" + problemFileName;
		String agentADDLPath = Globals.OUTPUT_TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";
		String heuristic = "saFF-glcl";
		int recursionLevel = -1;
		double timeLimitMin = ((double) timeoutMS / 60000);

		MADLAPlanner planner = new MADLAPlanner(agentDomainPath, agentProblemPath, agentADDLPath, heuristic,
				recursionLevel, timeLimitMin, agentList, agentName);

		List<String> result = null;

		/*
		 * try { result = planner.plan(); } catch (OutOfMemoryError E) {
		 * num_agents_not_solved++; return null; }
		 */

		result = planner.plan();

		if (planner.isTimeout)
			num_agents_timeout++;

		if (planner.isNotSolved)
			num_agents_not_solved++;

		if (!FileDeleter.deleteTempFiles()) {
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

		LOGGER.info("Leader agent: " + leaderAgent);

		return leaderAgent;
	}

	private boolean ReadAgentsFile() {

		LOGGER.info("Agents file reading");

		ExecCommand ec = null;

		try {

			String domainName = domainFileName.substring(0, domainFileName.lastIndexOf("."));
			String problemName = problemFileName.substring(0, problemFileName.lastIndexOf("."));

			String scriptPath = Globals.PYTHON_SCRIPTS_FOLDER + "/" + AGENT_PARSER_SCRIPT;

			String cmd = scriptPath + " " + Globals.INPUT_AGENTS_PATH + " " + domainName + " " + problemName;

			LOGGER.info("Running: " + cmd);

			// pr = Runtime.getRuntime().exec(cmd);
			// pr.waitFor();

			ec = new ExecCommand(cmd);
		} catch (Exception e) {
			LOGGER.info(e, e);
			return false;
		}

		// }
		// catch (Exception e) {
		// LOGGER.fatal(e, e);
		// return false;
		// }

		return agentsReaderValid(ec);
	}

	private boolean agentsReaderValid(ExecCommand ec) {

		LOGGER.info("Agents reading check");

		if (ec == null) {
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

		if (!configurationFile.exists()) {
			LOGGER.fatal(".json configuration file not exists");
			return false;
		}

		return true;
	}

	private Map<String, Set<String>> getVariableToValuesMapping(boolean format) {

		LOGGER.info("Getting mapping");

		String problemFilesPath = Globals.INPUT_GROUNDED_PATH;

		VariableValueExtractor extractor = new VariableValueExtractor(domainFileName,problemFileName,problemFilesPath);	

		Map<String, Set<String>> res = extractor.getMapping(format);

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;	
		}

		return res;
	}

}
