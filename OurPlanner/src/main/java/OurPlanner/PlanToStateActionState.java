package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import Model.SASDomain;
import Model.SASParser;
import Model.SASPreprocessor;
import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.Domain;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;

public class PlanToStateActionState {

	private final static Logger LOGGER = Logger.getLogger(PlanToStateActionState.class);

	private static final String TRANSLATOR = Globals.PYTHON_SCRIPTS_FOLDER + "/translate/translate.py";
	private static final String CONVERTOR = Globals.PYTHON_SCRIPTS_FOLDER + "/ma-pddl/ma-to-pddl.py";

	private static final String TEMP_DIR_PATH = Globals.OUTPUT_TEMP_PATH;
	private static final String SAS_FILE_PATH = Globals.SAS_OUTPUT_FILE_PATH;

	private String domainFileName = "";
	private String problemFileName = "";
	private String problemFilesPath = "";

	private double timeoutInMS = 0;
	private long startTimeMs = 0;

	private Map<String, Problem> agentsToProblems = null;

	private SASPreprocessor preprocessor;

	public PlanToStateActionState(String domainFileName,String problemFileName, String problemFilesPath, 
			long startTimeMs, long timeoutInMS) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("PlanToStateActionState constructor");

		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.problemFilesPath = problemFilesPath;
		this.startTimeMs = startTimeMs;
		this.timeoutInMS = timeoutInMS;

		logInput();

	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("problemFilesPath: " + problemFilesPath);
		LOGGER.info("startTimeMs: " + startTimeMs);
		LOGGER.info("timeoutInMS: " + timeoutInMS);
	}

	public List<StateActionState> generateSASList(List<String> plan, int lastActionIndex) {

		LOGGER.info("Generating SAS list from plan up to 'lastOKAction'");

		List<StateActionState> res = new ArrayList<StateActionState>();

		if (!generateProblems())
			return null;
		
		if (plan.size() == 0)
			return null;

		State initialState = preprocessor.getGlobalInit();

		for (int i = 0; i <= lastActionIndex; i++)
			res.add(generateSASForStep(plan, i, initialState));
		
		if(lastActionIndex + 1 < plan.size())
			res.add(generateSASForStep(plan, lastActionIndex + 1, initialState));

		delelteTemporaryFiles();

		deleteSasFile();

		return res;
	}

	private StateActionState generateSASForStep(List<String> plan, int actionIndex, State currentState) {

		LOGGER.info("Generating SAS for single step");

		String actionStr = plan.get(actionIndex);

		String agentName = getActionOwnerFromAction(actionStr);

		Problem problem = agentsToProblems.get(agentName);

		Action action = getActionFromPlan(problem, actionStr);

		State pre = new State(currentState);

		action.transform(currentState);

		String actionName = action.getSimpleLabel();

		Map<Integer, Set<Integer>> varDomains = problem.getDomain().getVariableDomains();

		Set<String> preFacts = getStateFacts(pre, varDomains);
		Set<String> postFacts = getStateFacts(currentState, varDomains);

		StateActionState sas = new StateActionState(preFacts, actionName, agentName, postFacts);

		return sas;
	}

	private String getActionOwnerFromAction(String actionStr) {

		LOGGER.info("Extracting aftion owner from action " + actionStr);

		if(actionStr.contains(Globals.PARAMETER_INDICATION))
			actionStr = actionStr.replace(Globals.PARAMETER_INDICATION, " ");

		String[] split = actionStr.split(" ");

		String label = split[2];

		split = label.split(Globals.AGENT_INDICATION);

		return split[split.length-1];		
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

		LOGGER.info("Get action label formatted");

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

	/*
	private static Set<String> getStateFacts(State state) {

		LOGGER.info("Extracting facts from state " + state);

		Set<String> out = new HashSet<String>();

		int[] values = state.getValues();

		for(int var = 0; var < values.length; ++var){
			if(var >= 0){

				String newVal = Domain.valNames.get(values[var]).toString();

				out.add(newVal);
			}
		}

		return out;
	}
	 */

	private static Set<String> getStateFacts(State state, Map<Integer, Set<Integer>> varDomains) {

		LOGGER.info("Extracting facts from state " + state);

		Set<String> out = new HashSet<String>();

		int[] values = state.getValues();

		for(int var = 0; var < values.length; ++var){
			if(var >= 0){

				String newVal = Domain.valNames.get(values[var]).toString();

				if(newVal.startsWith(Globals.NONE_KEYWORD)) {
					for (int val : varDomains.get(var)) {
						newVal = Domain.valNames.get(val).toString();

						if(val!=values[var])
							out.add("Negated" + newVal);
					}
				}
				else
					out.add(newVal);
			}
		}

		return out;
	}


	private boolean generateProblems() {

		LOGGER.info("Generating agent-to-problem mapping");

		agentsToProblems = new HashMap<String, Problem>();

		String agentADDLPath = TEMP_DIR_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";

		if(!runConvert()) {
			LOGGER.info("Convert failure!");
			return false;
		}	

		File agentFile = new File(agentADDLPath);
		if (!agentFile.exists()) {
			LOGGER.info("Agent file " + agentADDLPath + " does not exist!");
			return false;
		}

		ADDLObject addl = new ADDLParser().parse(agentFile);

		if(domainFileName == null) domainFileName = SAS_FILE_PATH;
		if(problemFileName == null) problemFileName = SAS_FILE_PATH;

		if(!runTranslate()) {
			LOGGER.info("Translate failure");
			return false;
		}

		File sasFile = new File(SAS_FILE_PATH);
		if (!sasFile.exists()) {
			LOGGER.info("SAS file " + SAS_FILE_PATH + " does not exist!");
			return false;
		}

		SASParser parser = new SASParser(sasFile);
		SASDomain sasDom = parser.getDomain();

		preprocessor = new SASPreprocessor(sasDom, addl, startTimeMs, timeoutInMS);

		for (String agentName : addl.getAgentList()) {
			
			Problem problem = preprocessor.getProblemForAgent(agentName, startTimeMs, timeoutInMS);
			
			if(problem == null)
				return false;
			
			agentsToProblems.put(agentName, problem);
		}

		return true;
	}

	private boolean runTranslate(){

		LOGGER.info("Translating to sas");

		String domainFilePath = TEMP_DIR_PATH + "/" + domainFileName ;
		String problemFilePath = TEMP_DIR_PATH + "/" + problemFileName ;

		try {
			String cmd = TRANSLATOR + " " + domainFilePath + " " + problemFilePath + " " + SAS_FILE_PATH + " --ignore_unsolvable";			

			LOGGER.info("RUN: " + cmd);

			new ExecCommand(cmd);
		}
		catch (Exception e) {
			LOGGER.info(e,e);
			return false;
		}

		try (FileReader fr = new FileReader(SAS_FILE_PATH)) {

			File f = new File(SAS_FILE_PATH);

			BufferedReader br = new BufferedReader(fr);

			String line;

			if ((line = br.readLine()) != null) {

				LOGGER.debug("test: " + f.getAbsolutePath());

				if (line.equals("unsolvable"))   {         
					LOGGER.info("SAS file unsolvable!");
					return false;
				}
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

	private boolean runConvert(){

		LOGGER.info("Converting to pddl");

		String path = problemFilesPath;
		String domain = domainFileName.substring(0, domainFileName.lastIndexOf("."));
		String problem = problemFileName.substring(0, problemFileName.lastIndexOf("."));
		String output = TEMP_DIR_PATH;

		try {
			String cmd = CONVERTOR + " " + path + " " + domain + " " + problem + " " + output;
			LOGGER.info("RUN: " + cmd);

			new ExecCommand(cmd);
		}
		catch (Exception e) {
			LOGGER.info(e,e);
			return false;
		}

		problemFilesPath = TEMP_DIR_PATH;

		return true;
	}


	private void delelteTemporaryFiles() {

		LOGGER.info("Deleting temporary files");

		File temp = new File(TEMP_DIR_PATH);		
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

	private boolean deleteSasFile() {

		LOGGER.info("Deleting temporary files");

		File outputSAS = new File(SAS_FILE_PATH);		
		if(outputSAS.exists()) {
			LOGGER.info("Deleting "+ SAS_FILE_PATH +" file");
			outputSAS.delete();
		}

		return true;
	}


}