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
import cz.agents.madla.executor.PlanTestExecutor;

public class OurPlanner implements Creator  {

	/* Global variables */
	private final static Logger LOGGER = Logger.getLogger(OurPlanner.class);
	private final static int ARGS_NUM = 6;
	private final static int SCRIPT_SUCCESS = 0;
	private final static int RANDOM_SEED = 1;

	private static final String AGENT_PARSER_SCRIPT = "./Scripts/parse-agents.py";

	/* Class variables */
	private String groundedFolder = "";
	private String localViewFolder = "";
	private String domainFileName = "";
	private String problemFileName = "";
	private String agentsFilePath = "";

	private File localViewFile = null;
	private File groundedFile = null;
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

		if(!runAlgorithm())
		{
			LOGGER.fatal("Algorithem failure");
			System.exit(1);
		}
	}

	private boolean runAlgorithm() {

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

			leaderAgentPlan = planForAgent(currentLeaderAgent);

			if(leaderAgentPlan == null)
				continue;		

			if(verifyPlans(leaderAgentPlan))
				return true;

		}

		return false;
	}

	private boolean verifyPlans(List<String> plan) {

		LOGGER.info("Verifing plans");

		String groundedDomainPath = groundedFolder + "/" + domainFileName;
		String groundedProblemPath = groundedFolder + "/" + problemFileName;
		String agentADDLPath = Globals.TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";

		PlanVerifier planVerifier = new PlanVerifier(groundedDomainPath,groundedProblemPath
				,agentADDLPath);		

		ArrayList<String> agentsExceptLeader = new ArrayList<>(agentList);	
		//TODO agentsExceptLeader.remove(currentLeaderAgent);

		boolean isVerified = planVerifier.verifyPlan(plan, agentsExceptLeader);
		
		delelteTemporaryFiles();
		
		return isVerified;
	}

	private List<String> planForAgent(String agentName) {

		LOGGER.info("Planning for leader agent");

		String agentDomainPath = localViewFolder + "/" + agentName + "/" + domainFileName;
		String agentProblemPath = localViewFolder + "/" + agentName + "/" + problemFileName;
		String agentADDLPath = Globals.TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";
		String heuristic = "saFF-glcl";
		int recursionLevel = -1;
		int timeLimitMin = 10;

		MADLAPlanner planner = new MADLAPlanner(agentDomainPath, agentProblemPath, agentADDLPath,
				heuristic, recursionLevel, timeLimitMin, agentName);

		List<String> result = planner.plan();

		delelteTemporaryFiles();

		return result;
	}

	private void delelteTemporaryFiles() {

		LOGGER.info("Deleting temporary files");

		File temp = new File(Globals.TEMP_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'temp' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				System.exit(1);
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
			status = "Usage: <path to grounded .pddl files> <path to .pddl problem file> <path to .plan file>";	

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

		domainFileName = args[3];
		extension = domainFileName.substring(domainFileName.lastIndexOf(".") + 1);
		//Checks if input domain name is of .pddl type
		if (!extension.equals("pddl")) {
			LOGGER.fatal("provided domain file name not existing");
			return false;
		}

		problemFileName = args[4];
		extension = problemFileName.substring(problemFileName.lastIndexOf(".") + 1);
		//Checks if input problem name is of .pddl type
		if (!extension.equals("pddl")) {
			LOGGER.fatal("provided problem file name not existing");
			return false;
		}

		agentsFilePath = args[5];
		agentsFile = new File(agentsFilePath);

		if( !agentsFile.exists()) {
			LOGGER.fatal("provided path to .agents file not existing");
			return false;
		}

		return true;
	}
}
