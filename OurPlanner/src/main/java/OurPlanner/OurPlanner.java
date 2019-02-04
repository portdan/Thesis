package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
	private final static int ARGS_NUM = 6;
	private final static int SCRIPT_SUCCESS = 0;
	private final static int RANDOM_SEED = 1;

	private static final String AGENT_PARSER_SCRIPT = "./Scripts/parse-agents.py";

	/* Class variables */
	private String trajectoriesFolder = "";
	private String groundedFolder = "";
	private String localViewFolder = "";
	private String domainFileName = "";
	private String problemFileName = "";
	private String agentsFilePath = "";

	private File trajectoriesFile = null;
	private File groundedFile = null;
	private File localViewFile = null;
	private File domainFile = null;
	private File problemFile = null;
	private File agentsFile = null;

	private List<String> agentList = null;
	String currentLeaderAgent = "";
	private List<String> availableLeaders= null;

	Random rnd = new Random(RANDOM_SEED);

	@Override
	public void create() {

		LOGGER.info("OurPlanner end");

	}

	@Override
	public void init(String[] args) {

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

		if(!runPlanningAlgorithm())
		{
			LOGGER.fatal("Planning algorithem failure");
			System.exit(1);
		}
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

			boolean isLearning = learnFromTrajectories(currentLeaderAgent);

			leaderAgentPlan = planForAgent(currentLeaderAgent, isLearning);

			if(leaderAgentPlan == null)
				continue;		

			if(verifyPlan(leaderAgentPlan, isLearning))
				return true;
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


	private boolean verifyPlan(List<String> plan, boolean isLearning) {

		LOGGER.info("Verifing plan");

		String localViewPath = "";

		if (isLearning) 
			localViewPath = Globals.LEARNED_PATH;
		else 
			localViewPath = this.localViewFolder;

		PlanVerifier planVerifier = new PlanVerifier(agentList,domainFileName,problemFileName,localViewPath,groundedFolder);		

		boolean isVerified = planVerifier.verifyPlan(plan,0);

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
		int timeLimitMin = 10;

		MADLAPlanner planner = new MADLAPlanner(agentDomainPath, agentProblemPath, agentADDLPath,
				heuristic, recursionLevel, timeLimitMin,agentList, agentName);

		List<String> result = planner.plan();

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

		Process pr = null;

		try {

			String domainName = domainFileName.substring(0,domainFileName.lastIndexOf(".") );
			String problemName = problemFileName.substring(0,problemFileName.lastIndexOf(".") );

			String cmd = AGENT_PARSER_SCRIPT + " " + agentsFilePath + " " + domainName + " " + problemName;

			LOGGER.info("Running: " + cmd);

			pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();

		} 
		catch (Exception e) {
			LOGGER.fatal(e, e);
			return false;
		}

		return agentsReaderValid(pr);
	}

	private boolean agentsReaderValid(Process prc) {

		LOGGER.info("Agents reading check");

		if(prc == null) {
			LOGGER.fatal("agent-parser.py script failure");
			return false;
		}

		List<String> output = new ArrayList<String>();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(prc.getInputStream()))) {                                
			String line;                                                                                                         
			while ((line = br.readLine()) != null)  {                                                                            
				output.add(line);                                                                                       
			}                                                                                                                    
		} catch (Exception e) {
			LOGGER.fatal(e, e);
			return false;
		}

		if(prc.exitValue() == SCRIPT_SUCCESS) {
			agentList = new ArrayList<>(output);
			LOGGER.info("Agents are: " + agentList);
			return true;
		}
		else {
			LOGGER.fatal(output);
			return false;
		}
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

		domainFileName = args[4];
		extension = domainFileName.substring(domainFileName.lastIndexOf(".") + 1);
		//Checks if input domain name is of .pddl type
		if (!extension.equals("pddl")) {
			LOGGER.fatal("provided domain file name not existing");
			return false;
		}

		problemFileName = args[5];
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
}
