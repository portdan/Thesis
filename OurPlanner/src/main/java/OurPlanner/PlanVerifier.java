package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

	private String domainFileName = "";
	private String problemFileName = "";
	private String sasFileName = "";
	private String localViewPath = "";

	private List<String> agentList = null;

	private boolean sasSolvable = false;

	State agentInitState = null;
	SuperState agentGoalState = null;

	Map<String,Integer> agentValCodes = new HashMap<String,Integer>();

	Map<String,String> agentProblemPDDL = new HashMap<String,String>();

	public PlanVerifier(List<String> agentList, String domainFileName,
			String problemFileName,	String localViewPath) {

		LOGGER.info("PlanVerifier constructor");

		sasFileName = SAS_FILE_NAME;
		this.agentList = agentList;
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.localViewPath = localViewPath;

		logInput();

		if(!preparePDDls()) {
			LOGGER.info("prepare PDDls failure");
			return;
		}

		if(!readProblemPDDls()) {
			LOGGER.info("read PDDls failure");
			return;
		}
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentList: " + agentList);
		LOGGER.info("sasFileName: " + sasFileName);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("localViewPath: " + localViewPath);

	}

	private boolean preparePDDls() {

		LOGGER.info("Preparing pddl problems");

		for (String agentName : agentList) {

			String groundedDomainPath = localViewPath + "/"  + agentName + "/" + domainFileName;
			String groundedProblemPath = localViewPath + "/" + agentName + "/" + problemFileName;
			String outputFolder = TEMP + "/" + agentName;

			LOGGER.info("Generating problem for agent: " + agentName);

			if(!runConvert(groundedDomainPath,groundedProblemPath,outputFolder)) {
				LOGGER.info("Convert failure - agent " + agentName);
				return false;
			}

			if(!isConversionSuccessful(outputFolder)){
				return false;
			}
		}

		return true;
	}

	private boolean isConversionSuccessful(String outputFolder) {

		LOGGER.info("Checking conversion");

		String newADDLPath = outputFolder + "/" + problemFileName.split("\\.")[0] + ".addl";		
		String newDomain = outputFolder + "/" + domainFileName;		
		String newProblem = outputFolder + "/" + domainFileName;

		if (!new File(newADDLPath).exists()) {
			LOGGER.info("File " + newADDLPath + " does not exist!");
			return false;
		}

		if (!new File(newDomain).exists()) {
			LOGGER.info("File " + newDomain + " does not exist!");
			return false;
		}

		if (!new File(newProblem).exists()) {
			LOGGER.info("File " + newProblem + " does not exist!");
			return false;
		}

		return true;
	}

	private boolean readProblemPDDls() {

		LOGGER.info("Reading pddl problems");

		for (String agentName : agentList) {

			LOGGER.info("Reading problem for agent: " + agentName);

			String ProblemPath = TEMP + "/" + agentName + "/" + problemFileName;
			String fileStr = "";

			try {
				fileStr = FileUtils.readFileToString(new File(ProblemPath),Charset.defaultCharset());
			} catch (IOException e) {
				LOGGER.info(e,e);
				return false;
			}

			if(!fileStr.isEmpty()) {
				agentProblemPDDL.put(agentName, fileStr);
			}
			else {
				LOGGER.info("PDDL " + ProblemPath + " is empty!");
				return false;
			}
		}

		return true;
	}

	private Problem generateProblem(String agentName, String groundedDomainPath,
			String groundedProblemPath, String agentADDLPath) {

		LOGGER.info("Generating problem for agent: " + agentName);

		File agentFile = new File(agentADDLPath);
		if (!agentFile.exists()) {
			LOGGER.info("Agent file " + agentADDLPath + " does not exist!");
			return null;
		}

		ADDLObject addl = new ADDLParser().parse(agentFile);

		if(domainFileName == null) domainFileName = sasFileName;
		if(problemFileName == null) problemFileName = sasFileName;

		if(!runTranslate(agentName)) {
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
		SASPreprocessor preprocessor = new SASPreprocessor(sasDom, addl);

		agentInitState = preprocessor.getGlobalInit();
		agentGoalState = preprocessor.getGlobalGoal();

		agentValCodes = preprocessor.valCodes;

		return preprocessor.getProblemForAgent(agentName);
	}

	private boolean runConvert(String groundedDomainPath, String groundedProblemPath, String outputFolder){

		LOGGER.info("Converting to pddl");

		String path = groundedDomainPath.substring(0, groundedDomainPath.lastIndexOf("/"));
		String domain = groundedDomainPath.substring(groundedDomainPath.lastIndexOf("/")+1, groundedDomainPath.lastIndexOf("."));
		String problem = groundedProblemPath.substring(groundedProblemPath.lastIndexOf("/")+1, groundedProblemPath.lastIndexOf("."));

		try {
			String cmd = CONVERTOR + " " + path + " " + domain + " " + problem + " " + outputFolder;
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

	private boolean runTranslate(String agentName){

		LOGGER.info("Translating to sas");

		try {

			String tempDomainPDDL = TEMP + "/" + agentName +"/" + domainFileName;
			String tempProblemPDDL = TEMP + "/" + agentName +"/" + problemFileName;

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

	public boolean verifyPlan(List<String> plan, int actionIndex) {

		LOGGER.info("Verifying plan");

		String actionStr = plan.get(actionIndex);

		String agentName = getAgentFromAction(actionStr);

		String groundedDomainPath = TEMP + "/"  + agentName + "/" + domainFileName;
		String groundedProblemPath = TEMP + "/" + agentName + "/" + problemFileName;
		String agentADDLPath = TEMP + "/" + agentName + "/" + problemFileName.split("\\.")[0] + ".addl";		

		Problem agentPoblem = generateProblem(agentName, groundedDomainPath, groundedProblemPath, agentADDLPath);

		Action action = getActionFromPlan(agentPoblem, actionStr);

		State state = new State(agentInitState);

		if(action != null) {

			if(action.isApplicableIn(state)){
				action.transform(state);
			}
			else {
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

			if(state.unifiesWith(agentGoalState)) {

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

			writeNewPDDLProblems(state);

			deleteSASfiles();

			return verifyPlan(plan, actionIndex + 1);
		}
	}

	private Action getActionFromPlan(Problem agentPoblem, String actionStr) {

		LOGGER.info("Extracting action object from action " + actionStr);

		String[] split = actionStr.split(" ");

		int hash = Integer.parseInt(split[split.length-1]);

		Action action = agentPoblem.getAction(hash);

		String label = split[2];
		for (int i = 3; i < split.length-1; i++)
			label += " " + split[i];

		for (Action act : agentPoblem.getAllActions()) {
			if(act.getSimpleLabel().equals(label))
				action = act;
		}

		return action;		
	}

	private String getAgentFromAction(String actionStr) {
		
		LOGGER.info("Extracting agent name from action " + actionStr);

		String[] split = actionStr.split(" ");

		String agent = split[1];

		return agent;		
	}

	private void writeNewPDDLProblems(State state) {

		LOGGER.info("Writing new PDDL problem files");

		HashMap<String, String> oldnewFacts = getOldNewFacts(state);

		for (String agentName : agentList) {

			try {

				String oldPDDL = agentProblemPDDL.get(agentName);

				int initIndex = oldPDDL.indexOf(Globals.INIT_KEYWORD);
				int goalIndex = oldPDDL.indexOf(Globals.GOAL_KEYWORD);

				String initSection = oldPDDL.substring(initIndex,goalIndex);

				Iterator<Entry<String, String>> it = oldnewFacts.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, String> pair = (Entry<String, String>)it.next();

					String oldFact = pair.getKey();
					String newFact = pair.getValue();

					initSection = initSection.replaceAll(oldFact, newFact);
				}

				String newPDDL = oldPDDL.substring(0,initIndex) 
						+ initSection 
						+ oldPDDL.substring(goalIndex,oldPDDL.length());

				agentProblemPDDL.put(agentName, newPDDL);
				
				String ProblemPath = TEMP + "/" + agentName + "/" + problemFileName;

				FileUtils.writeStringToFile(new File(ProblemPath), newPDDL, Charset.defaultCharset());


			} catch (Exception e) {
				LOGGER.info(e,e);
			}
		}
	}

	private HashMap<String, String> getOldNewFacts(State state) {

		HashMap<String, String> oldnewFacts = new HashMap<String,String>();

		for (int i = 0; i < state.getValues().length; i++) {

			if(state.getValue(i) != agentInitState.getValue(i)) {

				String oldFact = formatFact(agentInitState, i);
				String newFact = formatFact(state, i);

				oldnewFacts.put(oldFact, newFact);
			}
		}

		return oldnewFacts;
	}

	private String formatFact(State state, int i) {

		String a = getKeyFromValue(agentValCodes, state.getValue(i));
		String[] words = null;

		a = a.replaceAll("\\s+","");
		words = a.split("[ \n\t\r.,;:!?(){\\s+]");
		a = words[0];
		for (int j = 1; j < words.length; j++)
			a += " " + words[j];
		return a;
	}

	private String getKeyFromValue(Map<String,Integer> hm, int value) {
		for (String o : hm.keySet()) {
			if (hm.get(o).equals(value)) {
				return o;
			}
		}
		return null;
	}

	private void deleteSASfiles() {

		LOGGER.info("Deleting sas files");

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