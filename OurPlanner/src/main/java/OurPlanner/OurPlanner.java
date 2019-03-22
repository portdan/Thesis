package OurPlanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import cz.agents.alite.creator.Creator;

public class OurPlanner implements Creator  {

	/* Global variables */
	private final static Logger LOGGER = Logger.getLogger(OurPlanner.class);
	private final static int ARGS_NUM = 8;
	private final static int SEED = 1;

	private static final String AGENT_PARSER_SCRIPT = "./Scripts/parse-agents.py";

	/* Class variables */
	private String trajectoriesFolder = "";
	private String OutputFolder = "";
	private String OutputTestFolder = "";
	private String groundedFolder = "";
	private String localViewFolder = "";
	private String domainFileName = "";
	private String problemFileName = "";
	private String agentsFilePath = "";

	private File trajectoriesFile = null;
	private File OutputFile = null;
	private File OutputTestFile = null;
	private File groundedFile = null;
	private File localViewFile = null;
	private File domainFile = null;
	private File problemFile = null;
	private File agentsFile = null;

	private List<String> agentList = null;
	private	String currentLeaderAgent = "";
	private List<String> availableLeaders= null;

	// uses seed so agent order is same
	private	Random rnd = new Random(SEED);

	private int numOfTrajectories = 0;

	private int num_agents_solved = 0;
	private int num_agents_not_solved = 0;
	private int num_agents_timeout = 0;

	@Override
	public void create() {

		LOGGER.info("OurPlanner create");
	}

	@Override
	public void init(String[] args) {

		long startTime = System.currentTimeMillis();

		LOGGER.info("OurPlanner start");

		if(!ParseArgs(args))
		{
			LOGGER.fatal("Args not valid");
			System.exit(1);
		}

		if(!ReadAgentsFile())
		{
			LOGGER.fatal("Agents file reading failure");
			System.exit(1);
		}

		TestDataAccumulator.startNewAccumulator(domainFileName, problemFileName, agentList);

		TestDataAccumulator.getAccumulator().setOutputFile(OutputTestFolder);


		if(!deleteLearnedFiles()) {
			LOGGER.info("Deleting Learned files failure");
			System.exit(1);
		}

		if(!deleteTempFiles()) {
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

		runPlanningAlgorithm();

		TestDataAccumulator.getAccumulator().numOfAgentsSolved = num_agents_solved;
		TestDataAccumulator.getAccumulator().numOfAgentsTimeout = num_agents_timeout;
		TestDataAccumulator.getAccumulator().numOfAgentsNotSolved = num_agents_not_solved;		

		if(!copyOutputFolder()) {
			LOGGER.info("Coping output folder failure");
			System.exit(1);
		}

		if(!deleteOutputFiles()) {
			LOGGER.info("Deleting Output files failure");
			System.exit(1);
		}

		long finishTime = System.currentTimeMillis();

		TestDataAccumulator.getAccumulator().totalTimeMs = finishTime-startTime;

		TestDataAccumulator.getAccumulator().writeOutput();

		LOGGER.info(TestDataAccumulator.getAccumulator().toString());
	}

	private boolean runPlanningAlgorithm() {

		LOGGER.info("Running planning algorithm");

		availableLeaders = new ArrayList<>(agentList);

		List<String> leaderAgentPlan = null;

		int rounds = 0;

		while(leaderAgentPlan == null) {

			LOGGER.info("Round " + ++rounds + " - Start!");

			if(availableLeaders.isEmpty()){

				LOGGER.fatal("No more available leaders. No solution!");
				break;
			}

			currentLeaderAgent = pickLeader();

			long learningStartTime = System.currentTimeMillis();

			boolean isLearning = learnFromTrajectories(currentLeaderAgent);

			long learningFinishTime = System.currentTimeMillis();
			
			TestDataAccumulator.getAccumulator().agentLearningTimeMs.put(currentLeaderAgent, learningFinishTime - learningStartTime);

			long planningStartTime = System.currentTimeMillis();

			leaderAgentPlan = planForAgent(currentLeaderAgent, isLearning);

			long planningFinishTime = System.currentTimeMillis();
			
			TestDataAccumulator.getAccumulator().agentPlanningTimeMs.put(currentLeaderAgent, planningFinishTime - planningStartTime);

			if(leaderAgentPlan == null)
				continue;		

			if(verifyPlan(leaderAgentPlan, isLearning)) {

				TestDataAccumulator.getAccumulator().solvingAgent = currentLeaderAgent;
				TestDataAccumulator.getAccumulator().planLength= leaderAgentPlan.size();

				return true;
			}
		}

		return false;
	}

	private boolean learnFromTrajectories(String agentName) {

		LOGGER.info("Running learning algorithm");

		TrajectoryLearner learner = new TrajectoryLearner(agentList,agentName,trajectoriesFile, 
				groundedFile, localViewFile, domainFileName, problemFileName);	

		boolean isLearned = learner.learnNewActions();

		if(!deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return false;
		}

		return isLearned;

	}

	private boolean copyOriginalProblem() {

		LOGGER.info("Copy the original problem files");

		File destDir = new File(Globals.LEARNED_PATH);

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
			FileUtils.copyDirectory(srcDir, OutputFile);
		} catch (IOException e) {
			LOGGER.fatal(e, e);
			return false;
		}

		return true;
	}


	private boolean verifyPlan(List<String> plan, boolean isLearning) {

		LOGGER.info("Verifing plan");

		String localViewPath = "";

		if (isLearning) 
			localViewPath = Globals.LEARNED_PATH;
		else 
			localViewPath = this.localViewFolder;

		PlanVerifier planVerifier = new PlanVerifier(agentList,domainFileName,problemFileName,localViewPath,groundedFolder);		

		boolean isVerified = planVerifier.verifyPlan(plan,0);

		if(isVerified)
			num_agents_solved++;

		if(!deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return false;	
		}

		return isVerified;
	}

	private List<String> planForAgent(String agentName, boolean isLearning) {

		LOGGER.info("Planning for leader agent");

		String agentDomainPath = "";

		if (isLearning) 
			agentDomainPath = Globals.LEARNED_PATH  + "/" + agentName + "/" + domainFileName;
		else 
			agentDomainPath = localViewFolder + "/" + agentName + "/" + domainFileName;

		String agentProblemPath = localViewFolder + "/" + agentName + "/" + problemFileName;
		String agentADDLPath = Globals.TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";
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

		if(!deleteTempFiles()) {
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

			String cmd = AGENT_PARSER_SCRIPT + " " + agentsFilePath + " " + domainName + " " + problemName;

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
		LOGGER.info("Agents are: " + agentList);

		return true;
	}

	private boolean ParseArgs(String[] args) {

		LOGGER.info("Args validity check");

		String status = "";
		boolean valid = true;

		if ((valid = ArgsLenghtValid(args)) == false) 
			status = "Usage: <grounded folder> <localview folder> <trajectories folder> <domain name> <problem name> <addl name>";	

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

		String extension = "";
		String problemName = "";
		String domainName = "";

		groundedFolder = args[1];
		groundedFile = new File(groundedFolder);

		if( !groundedFile.exists()) {
			LOGGER.fatal("provided path to grounded folder not existing");
			return false;
		}

		localViewFolder = args[2];
		localViewFile = new File(localViewFolder);

		if( !localViewFile.exists()) {
			LOGGER.fatal("provided path to local view folder not existing");
			return false;
		}

		trajectoriesFolder = args[3];
		trajectoriesFile = new File(trajectoriesFolder);

		if( !trajectoriesFile.exists()) {
			LOGGER.fatal("provided path to trajectories folder not existing");
			return false;
		}

		numOfTrajectories = trajectoriesFile.listFiles().length;

		domainFileName = args[4];
		domainName = domainFileName.substring(0, domainFileName.lastIndexOf("."));
		extension = domainFileName.substring(domainFileName.lastIndexOf(".") + 1);
		//Checks if input domain name is of .pddl type
		if (!extension.equals("pddl")) {
			LOGGER.fatal("provided domain file name not existing");
			return false;
		}

		problemFileName = args[5];
		problemName = problemFileName.substring(0, problemFileName.lastIndexOf("."));
		extension = problemFileName.substring(problemFileName.lastIndexOf(".") + 1);
		//Checks if input problem name is of .pddl type
		if (!extension.equals("pddl")) {
			LOGGER.fatal("provided problem file name not existing");
			return false;
		}

		agentsFilePath = args[6];
		agentsFile = new File(agentsFilePath);

		if( !agentsFile.exists()) {
			LOGGER.fatal("provided path to .agents file not existing");
			return false;
		}

		OutputFolder = args[7] + "/" + problemName + "/" + numOfTrajectories + "_Trajectories";
		OutputFile = new File(OutputFolder);

		if( !OutputFile.exists()) {
			OutputFile.mkdir();
		}

		OutputTestFolder = args[8];
		OutputTestFile = new File(OutputTestFolder);

		if( !OutputTestFile.exists()) {
			OutputTestFile.getParentFile().mkdirs();
		}

		return true;
	}

	private boolean deleteTempFiles() {

		LOGGER.info("Deleting temporary files");

		File temp = new File(Globals.TEMP_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'temp' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				return false;
			}
		}

		File output = new File("output");		
		if(output.exists()) {
			LOGGER.info("Deleting 'output' file");
			output.delete();
		}

		File outputSAS = new File("output.sas");		
		if(outputSAS.exists()) {
			LOGGER.info("Deleting 'output.sas' file");
			outputSAS.delete();
		}

		return true;
	}

	private boolean deleteLearnedFiles() {

		LOGGER.info("Deleting learned files");

		File temp = new File(Globals.LEARNED_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'learned' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				return false;
			}
		}

		return true;
	}

	private boolean deleteOutputFiles() {

		LOGGER.info("Deleting output files");

		File temp = new File(Globals.OUTPUT_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'output' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				return false;
			}
		}

		return true;
	}
}
