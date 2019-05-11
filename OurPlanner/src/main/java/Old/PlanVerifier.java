package Old;

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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import Model.*;
import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.model.*;
import OurPlanner.*;


public class PlanVerifier {

	private final static Logger LOGGER = Logger.getLogger(PlanVerifier.class);

	private static final String TRANSLATOR = Globals.PYTHON_SCRIPTS_FOLDER + "/translate/translate.py";
	private static final String PREPROCESSOR = Globals.PYTHON_SCRIPTS_FOLDER + "/preprocess/preprocess-runner";
	private static final String CONVERTOR = Globals.PYTHON_SCRIPTS_FOLDER + "/ma-pddl/ma-to-pddl.py";

	private static final String OUTPUT_FILE_NAME = Globals.PROCESSED_SAS_OUTPUT_FILE_PATH;
	private static final String SAS_FILE_PATH = Globals.SAS_OUTPUT_FILE_PATH;
	private static final String TEMP_DIR_PATH = Globals.TEMP_PATH;

	private String domainFileName = "";
	private String problemFileName = "";
	private String problemFilesPath = "";

	private boolean useGrounded = false;

	private List<String> agentList = null;

	private boolean sasSolvable = false;

	State agentInitState = null;
	SuperState agentGoalState = null;

	Map<String,Integer> agentValCodes = new HashMap<String,Integer>();

	Map<String,String> agentProblemPDDL = new HashMap<String,String>();
	String groundedProblemPDDL = "";

	public PlanVerifier(List<String> agentList, String domainFileName,
			String problemFileName,	String problemFilesPath, boolean useGrounded) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("PlanVerifier constructor");

		this.agentList = agentList;
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.problemFilesPath = problemFilesPath;
		this.useGrounded = useGrounded;

		logInput();

		if(useGrounded) {
			if(!preparePDDLGrounded()) {
				LOGGER.info("prepare PDDls failure");
				return;
			}

			if(!readProblemPDDLGrounded()) {
				LOGGER.info("read PDDls failure");
				return;
			}
		}
		else {

			if(!preparePDDLLocalView()) {
				LOGGER.info("prepare PDDls failure");
				return;
			}

			if(!readProblemPDDLLocalView()) {
				LOGGER.info("read PDDls failure");
				return;
			}
		}
	}


	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentList: " + agentList);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("problemFilesPath: " + problemFilesPath);
		LOGGER.info("useGrounded: " + useGrounded);

	}

	private boolean preparePDDLLocalView() {

		LOGGER.info("Preparing pddl problems");

		for (String agentName : agentList) {

			String groundedDomainPath = problemFilesPath + "/"  + agentName + "/" + domainFileName;
			String groundedProblemPath = problemFilesPath + "/" + agentName + "/" + problemFileName;
			String outputFolder = TEMP_DIR_PATH + "/" + agentName;

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

	private boolean preparePDDLGrounded() {

		LOGGER.info("Preparing pddl grounded problem");

		String groundedDomainPath = problemFilesPath + "/" + domainFileName;
		String groundedProblemPath = problemFilesPath + "/" + problemFileName;
		String outputFolder = TEMP_DIR_PATH;

		LOGGER.info("Generating problem");

		if(!runConvert(groundedDomainPath,groundedProblemPath,outputFolder)) {
			LOGGER.info("Convert failure!");
			return false;
		}

		if(!isConversionSuccessful(outputFolder)){
			return false;
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

	private boolean readProblemPDDLLocalView() {

		LOGGER.info("Reading pddl problems");

		for (String agentName : agentList) {

			LOGGER.info("Reading problem for agent: " + agentName);

			String ProblemPath = TEMP_DIR_PATH + "/" + agentName + "/" + problemFileName;
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

	private boolean readProblemPDDLGrounded() {

		LOGGER.info("Reading grounded pddl problem");

		String ProblemPath = TEMP_DIR_PATH + "/" + problemFileName;
		String fileStr = "";

		try {
			fileStr = FileUtils.readFileToString(new File(ProblemPath),Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return false;
		}

		if(!fileStr.isEmpty()) {
			groundedProblemPDDL = fileStr;
		}
		else {
			LOGGER.info("PDDL " + ProblemPath + " is empty!");
			return false;
		}

		return true;
	}

	private Problem generateProblem(String agentName, String domainPath,
			String problemPath, String agentADDLPath) {

		LOGGER.info("Generating problem for agent: " + agentName);

		File agentFile = new File(agentADDLPath);
		if (!agentFile.exists()) {
			LOGGER.info("Agent file " + agentADDLPath + " does not exist!");
			return null;
		}

		ADDLObject addl = new ADDLParser().parse(agentFile);

		if(domainFileName == null) domainFileName = SAS_FILE_PATH;
		if(problemFileName == null) problemFileName = SAS_FILE_PATH;

		if(!runTranslate(domainPath, problemPath)) {
			LOGGER.info("Translate failure");
			return null;
		}

		if (!sasSolvable) {
			LOGGER.info("Sas not Solvable. Plan not found!");
			return null;
		}

		/* PREPROCESS NOT NEEDED
		if(!runPreprocess()) {
			LOGGER.info("Preprocess failure");
			return null;
		}
		 */

		File sasFile = new File(SAS_FILE_PATH);
		if (!sasFile.exists()) {
			LOGGER.info("SAS file " + SAS_FILE_PATH + " does not exist!");
			return null;
		}

		SASParser parser = new SASParser(sasFile);
		SASDomain sasDom = parser.getDomain();
		PlanVerifierSASPreprocessor preprocessor = new PlanVerifierSASPreprocessor(sasDom, addl);

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
			//			Process pr = Runtime.getRuntime().exec(cmd);
			//			pr.waitFor();

			new ExecCommand(cmd);
		}
		catch (Exception e) {
			LOGGER.info(e,e);
			return false;
		}

		//		} catch (IOException e) {
		//			LOGGER.info(e,e);
		//			return false;
		//		} catch (InterruptedException e) {
		//			LOGGER.info(e,e);
		//			return false;
		//		}

		return true;
	}

	private boolean runTranslate(String domainPath, String problemPath){

		LOGGER.info("Translating to sas");

		try {
			String cmd = TRANSLATOR + " " + domainPath + " " + problemPath + " " + SAS_FILE_PATH + " --ignore_unsolvable";			

			LOGGER.info("RUN: " + cmd);
			//			Process pr = Runtime.getRuntime().exec(cmd);
			//			pr.waitFor();

			new ExecCommand(cmd);
		}
		catch (Exception e) {
			LOGGER.info(e,e);
			return false;
		}

		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		} catch (InterruptedException e) {
		//			e.printStackTrace();
		//		}

		try (FileReader fr = new FileReader(SAS_FILE_PATH)) {

			File f = new File(SAS_FILE_PATH);

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
			LOGGER.info("SAS file " + SAS_FILE_PATH + " does not exist!");
			return false;
		} catch (IOException e) {
			LOGGER.info("SAS file " + SAS_FILE_PATH + " bad!");
			return false;
		}

		return true;

	}

	private boolean runPreprocess() {

		LOGGER.info("Preprocessing sas file");

		try {
			String cmd = PREPROCESSOR;
			LOGGER.info("RUN: " + cmd);
			//			Process pr = Runtime.getRuntime().exec(cmd);
			//			pr.waitFor();

			new ExecCommand(cmd);
		}
		catch (Exception e) {
			LOGGER.info(e,e);
			return false;
		}

		//		} catch (Exception e) {
		//			LOGGER.info("Preprocess script error");
		//			e.printStackTrace();
		//			return false;
		//		} 

		return true;
	}

	public boolean verifyPlan(List<String> plan, int actionIndex) {

		LOGGER.info("Verifying plan");

		String actionStr = plan.get(actionIndex);

		String actionOwner = getActionOwnerFromAction(actionStr);

		String agentName = actionOwner; //getAgentFromAction(actionStr);

		String domainPath = "";
		String problemPath = "";
		String agentADDLPath = "";

		if(useGrounded) {
			domainPath = TEMP_DIR_PATH + "/" + domainFileName;
			problemPath = TEMP_DIR_PATH + "/" + problemFileName;
			agentADDLPath = TEMP_DIR_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";	
		}
		else {
			domainPath = TEMP_DIR_PATH + "/"  + agentName + "/" + domainFileName;
			problemPath = TEMP_DIR_PATH + "/" + agentName + "/" + problemFileName;
			agentADDLPath = TEMP_DIR_PATH + "/" + agentName + "/" + problemFileName.split("\\.")[0] + ".addl";	
		}		

		Problem agentProblem = generateProblem(agentName, domainPath, problemPath, agentADDLPath);

		Action action = getActionFromPlan(agentProblem, actionStr);

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

			if(useGrounded)
				writeNewPDDLProblemGrounded(state);
			else
				writeNewPDDLProblemLocalView(state);

			deleteSASfiles();

			return verifyPlan(plan, actionIndex + 1);
		}
	}

	private Action getActionFromPlan(Problem agentPoblem, String actionStr) {

		LOGGER.info("Extracting action object from action " + actionStr);

		String[] split = actionStr.split(" ");

		//int hash = Integer.parseInt(split[split.length-1]);

		Action action = null;//agentPoblem.getAction(hash);

		//String label = split[2];
		String label = GetLabelFormated(split).trim();	

		for (Action act : agentPoblem.getAllActions()) {
			if(act.getSimpleLabel().equals(label))
				action = act;
		}

		return action;		
	}

	private String GetLabelFormated(String[] split) {

		String label = split[2];

		if(label.contains(Globals.PARAMETER_INDICATION)){
			return label.replace(Globals.PARAMETER_INDICATION, " ");
		}
		else {
			for (int i = 3; i < split.length-1; i++)
				label += " " + split[i];

			return label;
		}
	}

	private String getAgentFromAction(String actionStr) {

		LOGGER.info("Extracting agent name from action " + actionStr);

		String[] split = actionStr.split(" ");

		String agent = split[1];

		return agent;		
	}

	private String getActionOwnerFromAction(String actionStr) {

		LOGGER.info("Extracting aftion owner from action " + actionStr);
		
		if(actionStr.contains(Globals.PARAMETER_INDICATION))
			actionStr = actionStr.replace(Globals.PARAMETER_INDICATION, " ");
		
		String[] split = actionStr.split(" ");

		String label = split[2];
		
		split = label.split("-");
		
		return split[split.length-1];		
	}

	private void writeNewPDDLProblemLocalView(State state) {

		LOGGER.info("Writing new PDDL problem files");

		HashMap<String, String> oldnewFacts = getOldNewFacts(state);

		for (String agentName : agentList) {

			try {

				String oldPDDL = agentProblemPDDL.get(agentName);

				int initIndex = oldPDDL.indexOf(Globals.INIT_KEYWORD) + Globals.INIT_KEYWORD.length();
				int goalIndex = oldPDDL.indexOf(Globals.GOAL_KEYWORD);

				String initSection = oldPDDL.substring(initIndex,goalIndex);

				Iterator<Entry<String, String>> it = oldnewFacts.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, String> pair = (Entry<String, String>)it.next();

					String oldFact = pair.getKey();
					String newFact = pair.getValue();

					if(oldFact == Globals.ADD_NEW_FACT_INDICATION)
						initSection = "\t(" + newFact + ")\n" + initSection;
					else if(newFact == Globals.REMOVE_OLD_FACT_INDICATION)
						initSection = initSection.replace("\t(" + oldFact + ")\n", "");
					else
						initSection = initSection.replaceAll(oldFact, newFact);
				}

				String newPDDL = oldPDDL.substring(0,initIndex) 
						+ initSection 
						+ oldPDDL.substring(goalIndex,oldPDDL.length());

				agentProblemPDDL.put(agentName, newPDDL);

				String ProblemPath = TEMP_DIR_PATH + "/" + agentName + "/" + problemFileName;

				FileUtils.writeStringToFile(new File(ProblemPath), newPDDL, Charset.defaultCharset());


			} catch (Exception e) {
				LOGGER.info(e,e);
			}
		}
	}

	private void writeNewPDDLProblemGrounded(State state) {

		LOGGER.info("Writing new grouned PDDL problem file");

		HashMap<String, String> oldnewFacts = getOldNewFacts(state);

		try {

			String oldPDDL = groundedProblemPDDL;

			int initIndex = oldPDDL.indexOf(Globals.INIT_KEYWORD) + Globals.INIT_KEYWORD.length();
			int goalIndex = oldPDDL.indexOf(Globals.GOAL_KEYWORD);

			String initSection = oldPDDL.substring(initIndex,goalIndex);

			Iterator<Entry<String, String>> it = oldnewFacts.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> pair = (Entry<String, String>)it.next();

				String oldFact = pair.getKey();
				String newFact = pair.getValue();

				if(oldFact == Globals.ADD_NEW_FACT_INDICATION)
					initSection = "\t(" + newFact + ")\n" + initSection;
				else if(newFact == Globals.REMOVE_OLD_FACT_INDICATION)
					initSection = initSection.replace("\t(" + oldFact + ")\n", "");
				else
					initSection = initSection.replaceAll(oldFact, newFact);
			}

			String newPDDL = oldPDDL.substring(0,initIndex) 
					+ initSection 
					+ oldPDDL.substring(goalIndex,oldPDDL.length());

			groundedProblemPDDL = newPDDL;

			String ProblemPath = TEMP_DIR_PATH + "/" + problemFileName;

			FileUtils.writeStringToFile(new File(ProblemPath), newPDDL, Charset.defaultCharset());


		} catch (Exception e) {
			LOGGER.info(e,e);
		}

	}

	private HashMap<String, String> getOldNewFacts(State state) {

		HashMap<String, String> oldnewFacts = new HashMap<String,String>();

		for (int i = 0; i < state.getValues().length; i++) {

			if(state.getValue(i) != agentInitState.getValue(i)) {

				String oldFact = formatFact(agentInitState, i);
				String newFact = formatFact(state, i);

				if(oldFact.startsWith(Globals.NEGATED_KEYWORD) && 
						!newFact.startsWith(Globals.NEGATED_KEYWORD))
					oldFact = Globals.ADD_NEW_FACT_INDICATION;

				if(newFact.startsWith(Globals.NEGATED_KEYWORD))
					newFact = Globals.REMOVE_OLD_FACT_INDICATION;

				oldnewFacts.put(oldFact, newFact);
			}
		}

		return oldnewFacts;
	}

	private String formatFact(State state, int i) {

		String res = getKeyFromValue(agentValCodes, state.getValue(i));
		String[] words = null;

		res = res.replaceAll("\\s+","");
		words = res.split("[ \n\t\r.,;:!?(){\\s+]");
		res = words[0];
		for (int j = 1; j < words.length; j++)
			res += " " + words[j];

		return res;
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

		File output = new File(OUTPUT_FILE_NAME);		
		if(output.exists()) {
			LOGGER.info("Deleting " + OUTPUT_FILE_NAME + " file");
			output.delete();
		}

		File outputSAS = new File(SAS_FILE_PATH);		
		if(outputSAS.exists()) {
			LOGGER.info("Deleting " + SAS_FILE_PATH + " file");
			outputSAS.delete();
		}
	}

}