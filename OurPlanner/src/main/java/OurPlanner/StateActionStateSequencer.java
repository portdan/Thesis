package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;
import model.SASDomain;
import model.SASParser;
import model.SASPreprocessor;


public class StateActionStateSequencer {

	private final static Logger LOGGER = Logger.getLogger(StateActionStateSequencer.class);

	private static final String TRANSLATOR = "./Scripts/translate/translate.py";
	private static final String PREPROCESSOR = "./Scripts/preprocess/preprocess-runner";
	private static final String CONVERTOR = "./Scripts/ma-pddl/ma-to-pddl.py";

	private static final String TEMP = Globals.TEMP_PATH;
	private static final String SAS_FILE_NAME = "output.sas";

	private String agentName = "";
	private String domainFileName = "";
	private String problemFileName = "";
	private String convertedDomainPath = "";
	private String convertedProblemPath = "";
	private String sasFileName = "";
	private String groundedPath = "";

	private boolean sasSolvable = false;

	Problem problem = null;
	State currentState = null;

	public StateActionStateSequencer(String agentName, String groundedPath, String domainFileName,String problemFileName) {

		LOGGER.info("StateActionStateSequencer constructor");

		sasFileName = SAS_FILE_NAME;

		this.agentName = new String(agentName);
		this.groundedPath = new String(groundedPath);
		this.domainFileName = new String(domainFileName);
		this.problemFileName = new String(problemFileName);

		logInput();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentName: " + agentName);
		LOGGER.info("sasFileName: " + sasFileName);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("groundedPath: " + groundedPath);
	}

	public List<StateActionState> generateSequance(String trajectoryPath){

		LOGGER.info("Generating state action state sequence for " + trajectoryPath );

		List<StateActionState> res = new ArrayList<StateActionState>();

		String groundedDomainPath = groundedPath + "/" + domainFileName;
		String groundedProblemPath = groundedPath + "/" + problemFileName;
		String agentADDLPath = TEMP + "/" + problemFileName.split("\\.")[0] + ".addl";		

		problem = generateProblem(groundedDomainPath, groundedProblemPath, agentADDLPath);
		currentState = problem.initState;

		try (BufferedReader br = new BufferedReader(new FileReader(trajectoryPath))) {

			String line = br.readLine();
			while (line != null) {
				res.add(processAction(line));
				line = br.readLine();
			}

		} catch (Exception e) {
			LOGGER.info(e,e);
			return null;
		}

		deleteTempFiles();

		return res;
	}

	private Problem generateProblem(String groundedDomainPath, String groundedProblemPath, String agentADDLPath) {

		LOGGER.info("Generating problem");

		if(!runConvert()) {
			LOGGER.info("Convert failure");
			return null;
		}

		File agentFile = new File(agentADDLPath);
		if (!agentFile.exists()) {
			LOGGER.info("Agent file " + agentADDLPath + " does not exist!");
			return null;
		}

		ADDLObject addl = new ADDLParser().parse(agentFile);

		if(convertedDomainPath == null) convertedDomainPath = sasFileName;
		if(convertedProblemPath == null) convertedProblemPath = sasFileName;

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
		SASPreprocessor preprocessor = new SASPreprocessor(sasDom, addl);

		return preprocessor.getProblemForAgent(agentName);
	}

	private boolean runConvert(){

		LOGGER.info("Converting to pddl");

		String path = groundedPath;
		String domain = domainFileName.substring(0, domainFileName.lastIndexOf("."));
		String problem = problemFileName.substring(0, problemFileName.lastIndexOf("."));


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

		convertedDomainPath = TEMP + "/" + domain + ".pddl";
		convertedProblemPath = TEMP + "/" + problem + ".pddl";

		return true;
	}

	private boolean runTranslate(){

		LOGGER.info("Translating to sas");

		try {
			String cmd = TRANSLATOR + " " + convertedDomainPath + " " + convertedProblemPath + " --ignore_unsolvable";
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

	private StateActionState processAction(String line) {

		LOGGER.info("Processing action " + line);

		State pre = new State(currentState);
		Action action = getActionFromPlan(problem, line);

		if(action != null) {

			if(action.isApplicableIn(currentState))
				action.transform(currentState);

			return new StateActionState(pre, action, currentState);
		}
		else
			return null;
	}

	private Action getActionFromPlan(Problem agentPoblem, String actionStr) {

		LOGGER.info("Extracting action object from action " + actionStr);

		int start = actionStr.indexOf('(');
		int end = actionStr.indexOf(')');

		if(start == -1 || end == -1)
			return null;

		String label = actionStr.substring(start+1, end);

		Action action = null;

		for (Action act : agentPoblem.getAllActions()) {
			if(act.getSimpleLabel().equals(label))
				action = act;
		}

		return action;		
	}

	private void deleteTempFiles() {

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