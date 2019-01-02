package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.model.*;

import model.*;

public class PlanVerifier {

	private final static Logger LOGGER = Logger.getLogger(PlanVerifier.class);

	private static final String TRANSLATOR = "./Scripts/translate/translate.py";
	private static final String PREPROCESSOR = "./Scripts/preprocess/preprocess-runner";
	private static final String CONVERTOR = "./Scripts/ma-pddl/ma-to-pddl.py";

	private static final String TEMP = Globals.TEMP_PATH;
	private static final String SAS_FILE_NAME = "output";

	private static final boolean LV_MODE = true;


	private String domainFileName = "";
	private String problemFileName = "";
	private String sasFileName = "";
	private String localViewPath = "";
	private String groundedDomainPath = "";
	private String groundedProblemPath = "";

	private List<String> agentList = null;

	private boolean sasSolvable = false;

	protected SASPreprocessor preprocessor = null;

	Problem p = null;

	Map<String,Problem> problems = new HashMap<String,Problem>();
	Map<String,State> states = new HashMap<String,State>();
	Map<String,SuperState> goalStates = new HashMap<String,SuperState>();

	Map<String,Map<String,Set<String>>> agentVarVals = new HashMap<String,Map<String,Set<String>>>();
	Map<String,Map<String,Integer>> agentVarCodes = new HashMap<String,Map<String,Integer>>();
	Map<String,Map<String,Integer>> agentValCodes = new HashMap<String,Map<String,Integer>>();


	public PlanVerifier(String groundedDomainPath, String groundedProblemPath,
			List<String> agentList, String domainFileName, String problemFileName,
			String groundedPath) {

		LOGGER.info("PlanVerifier constructor");

		sasFileName = SAS_FILE_NAME;
		this.groundedDomainPath = groundedDomainPath;
		this.groundedProblemPath = groundedProblemPath;
		this.agentList = agentList;
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.localViewPath = groundedPath;

		logInput();

		if(LV_MODE)
			generateProblemsFromLV();
		else
			generateProblemsFromGrounded();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentList: " + agentList);
		LOGGER.info("sasFileName: " + sasFileName);
		LOGGER.info("groundedDomainPath: " + groundedDomainPath);
		LOGGER.info("groundedProblemPath: " + groundedProblemPath);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("localViewPath: " + localViewPath);

	}

	private void generateProblemsFromGrounded() {

		LOGGER.info("Generating problems");

		String agentADDLPath = Globals.TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";		

		for (String agentName : agentList) {
			problems.put(agentName, generateProblem(agentName,groundedDomainPath ,
					groundedProblemPath, agentADDLPath));
			delelteTemporaryFiles();
		}
	}
	private void generateProblemsFromLV() {

		LOGGER.info("Generating problems");

		for (String agentName : agentList) {

			String groundedDomainPath = localViewPath + "/"  + agentName + "/" + domainFileName;
			String groundedProblemPath = localViewPath + "/" + agentName + "/" + problemFileName;
			String agentADDLPath = Globals.TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";		

			Problem problem = generateProblem(agentName,groundedDomainPath , groundedProblemPath, agentADDLPath);

			problems.put(agentName, problem);

			delelteTemporaryFiles();
		}

	}

	private Problem generateProblem(String agentName, String groundedDomainPath,
			String groundedProblemPath, String agentADDLPath) {

		LOGGER.info("Generating problem for agent: " + agentName);

		if(!runConvert(groundedDomainPath,groundedProblemPath)) {
			LOGGER.info("Convert failure");
			return null;
		}

		File agentFile = new File(agentADDLPath);
		if (!agentFile.exists()) {
			LOGGER.info("Agent file " + agentADDLPath + " does not exist!");
			return null;
		}

		ADDLObject addl = new ADDLParser().parse(agentFile);

		if(domainFileName == null) domainFileName = sasFileName;
		if(problemFileName == null) problemFileName = sasFileName;

		if(!runTranslate()) {
			LOGGER.info("Translate failure");
			return null;
		}

		if (!sasSolvable) {
			LOGGER.info("Sas not Solvable. Plan not found!");
			return null;
		}

		if(!runPreprocess()) {
			LOGGER.info("Preprocess failure");
			return null;
		}

		File sasFile = new File(sasFileName);
		if (!sasFile.exists()) {
			LOGGER.info("SAS file " + sasFileName + " does not exist!");
			return null;
		}

		SASParser parser = new SASParser(sasFile);
		SASDomain sasDom = parser.getDomain();
		preprocessor = new SASPreprocessor(sasDom, addl);

		states.put(agentName, preprocessor.getGlobalInit());
		goalStates.put(agentName, preprocessor.getGlobalGoal());

		agentVarCodes.put(agentName, preprocessor.varCodes);
		agentValCodes.put(agentName, preprocessor.valCodes);

		agentVarVals.put(agentName,preprocessor.getAgentVarVals(agentName));

		return preprocessor.getProblemForAgent(agentName);
	}

	public boolean verifyPlan(List<String> plan, int actionIndex) {

		LOGGER.info("Verifying plan");

		String actionStr = plan.get(actionIndex);

		String agentName = getAgentFromAction(actionStr);

		Action action = getActionFromPlan(actionStr, agentName);

		if(action != null) {

			if(!verifyActionForAllAgents(action)) {
				LOGGER.info("Action " + action.getSimpleLabel() + " is not applicable!");
				return false;
			}
		}
		else {
			LOGGER.info("agent " + agentName + " is not the owner of this action - plan not verified!");
			return false;
		}

		//		if(actionIndex == plan.size() - 1){
		//			LOGGER.info("Plan verifing finished - plan verified!");
		//			return true;
		//		}
		//		if(state.unifiesWith(goalState)){
		//			LOGGER.info("Goal is reached - plan verified!");
		//			return true;
		//		}

		if(actionIndex == plan.size() - 1) {

			if(isGoalReached()){
				LOGGER.info("Goal is reached - plan verified!");
				return true;
			}
			else{
				LOGGER.info("Goal is not reached - plan not verified!");
				return false;
			}
		}
		else {

			LOGGER.info("verify next action");

			return verifyPlan(plan, actionIndex + 1);
		}
	}

	private boolean verifyActionForAllAgents(Action action) {

		LOGGER.info("Verifying action " + action.getSimpleLabel() + " of agent " + action);

		boolean applicable = false;

		for (String agentName : agentList) {

			State state = states.get(agentName);

			Action agentAction =  null;

			for (Action act : problems.get(agentName).getAllActions()) {
				if(act.equals(action))
					agentAction = act;
			}

			if(agentAction != null) {
				if(agentAction.isApplicableIn(state)){
					agentAction.transform(state);
					applicable = true;
				}
			}
		}

		return applicable;
	}

	private boolean isGoalReached() {

		LOGGER.info("checking if goal is reached for all agents");

		for (String agentName : agentList) {

			LOGGER.info("checking goal for agent " + agentName);

			State state = states.get(agentName);
			SuperState goal = goalStates.get(agentName);

			if(!state.unifiesWith(goal)) {

				LOGGER.info("agent " + agentName + " : state " + state + " not unifies with goal " + goal);

				return false;
			}
		}

		return true;
	}

	private Action getActionFromPlan(String actionStr, String agentName) {

		String[] split = actionStr.split(" ");

		int hash = Integer.parseInt(split[split.length-1]);

		Action action = problems.get(agentName).getAction(hash);
		
		String label = split[2];
		for (int i = 3; i < split.length-1; i++)
			label += " " + split[i];
		
		for (Action act : problems.get(agentName).getAllActions()) {
			if(act.getSimpleLabel().equals(label))
				action = act;
		}

		return action;		
	}

	private String getAgentFromAction(String actionStr) {

		String[] split = actionStr.split(" ");

		String agent = split[1];

		return agent;		
	}

	private boolean runConvert(String groundedDomainPath, String groundedProblemPath){

		LOGGER.info("Converting to pddl");

		String path = groundedDomainPath.substring(0, groundedDomainPath.lastIndexOf("/"));
		String domain = groundedDomainPath.substring(groundedDomainPath.lastIndexOf("/")+1, groundedDomainPath.lastIndexOf("."));
		String problem = groundedProblemPath.substring(groundedProblemPath.lastIndexOf("/")+1, groundedProblemPath.lastIndexOf("."));

		try {
			String cmd = CONVERTOR + " " + path + " " + domain + " " + problem + " " + TEMP;
			LOGGER.info("RUN: " + cmd);
			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();
		} catch (IOException e) {
			LOGGER.info(e,e);
			return false;
		} catch (InterruptedException e) {
			LOGGER.info(e,e);
			return false;
		}

		return true;
	}

	private boolean runTranslate(){

		LOGGER.info("Translating to sas");

		try {

			String tempDomainPDDL = TEMP + "/" + domainFileName;
			String tempProblemPDDL = TEMP + "/" + problemFileName;

			String cmd = TRANSLATOR + " " + tempDomainPDDL + " " + tempProblemPDDL + " --ignore_unsolvable";
			LOGGER.info("RUN: " + cmd);
			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String sasFileName = "output.sas";

		try (FileReader fr = new FileReader(sasFileName)) {

			File f = new File(sasFileName);

			BufferedReader br = new BufferedReader(fr);

			String line;

			if ((line = br.readLine()) != null) {

				LOGGER.debug("test: " + f.getAbsolutePath());

				if (line.equals("unsolvable"))            
					sasSolvable = false;
				else
					sasSolvable = true;
			}

		} catch (FileNotFoundException e) {
			LOGGER.info("SAS file " + sasFileName + " does not exist!");
			return false;
		} catch (IOException e) {
			LOGGER.info("SAS file " + sasFileName + " bad!");
			return false;
		}

		return true;

	}

	private boolean runPreprocess() {

		LOGGER.info("Preprocessing sas file");

		try {
			String cmd = PREPROCESSOR;
			LOGGER.info("RUN: " + cmd);
			Process pr = Runtime.getRuntime().exec(cmd);
			pr.waitFor();
		} catch (Exception e) {
			LOGGER.info("Preprocess script error");
			e.printStackTrace();
			return false;
		} 

		return true;
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

}