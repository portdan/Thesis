package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.Domain;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;
import model.StateActionStateSASPreprocessor;
import model.SASDomain;
import model.SASParser;


public class StateActionStateSequencer {

	private final static Logger LOGGER = Logger.getLogger(StateActionStateSequencer.class);

	private static final String TRANSLATOR = "./Scripts/translate/translate.py";
	private static final String PREPROCESSOR = "./Scripts/preprocess/preprocess-runner";
	private static final String CONVERTOR = "./Scripts/ma-pddl/ma-to-pddl.py";

	private static final String TEMP = Globals.TEMP_PATH;
	private static final String SAS_FILE_NAME = "output.sas";

	private List<String> agentList = null;
	private String domainFileName = "";
	private String problemFileName = "";
	private String convertedDomainPath = "";
	private String convertedProblemPath = "";
	private String sasFileName = "";
	private File problemFiles = null;
	private File trajectoryFiles = null;

	private boolean sasSolvable = false;

	State currentState = null;

	public StateActionStateSequencer(List<String> agentList, File problemFiles, 
			String domainFileName, String problemFileName, File trajectoryFiles ) {

		LOGGER.info("StateActionStateSequencer constructor");

		sasFileName = SAS_FILE_NAME;

		this.agentList = agentList;
		this.problemFiles = problemFiles;
		this.trajectoryFiles = trajectoryFiles;
		this.domainFileName = new String(domainFileName);
		this.problemFileName = new String(problemFileName);

		logInput();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentList: " + agentList);
		LOGGER.info("sasFileName: " + sasFileName);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("problemFiles: " + problemFiles);
		LOGGER.info("trajectoryFiles: " + trajectoryFiles);
	}
	/*
	public List<StateActionState> generateSequences() {

		LOGGER.info("Generating sequences");

		List<StateActionState> trajectorySequences = new ArrayList<StateActionState>();

		File[] TrajDir = trajectoryFiles.listFiles();
		File[] ProbDir = problemFiles.listFiles();

		if (TrajDir != null) 
			for (File trj : TrajDir) {

				String trajectoryPath = trj.getPath();
				String trajectoryName = FilenameUtils.getBaseName(trajectoryPath);

				String ext = FilenameUtils.getExtension(trajectoryPath); 

				if(ext.equals(Globals.TRAJECTORY_FILE_EXTENSION)) {

					LOGGER.info("Generating sequence for trajectory in " + trajectoryName);

					if (ProbDir != null) 
						for (File prb : ProbDir) {

							String problemPath = prb.getPath();
							String problemName = FilenameUtils.getBaseName(problemPath);

							if(problemName.equals(trajectoryName)) {	

								for (String agentName : agentList) {

									List<StateActionState> res = generateSequance(agentName, trajectoryPath);
									trajectorySequences.addAll(res);
								}


							}
						}
				}

			}

		return trajectorySequences;
	}
	 */

	public List<StateActionState> generateSequences() {

		LOGGER.info("Generating sequences");

		List<StateActionState> trajectorySequences = new ArrayList<StateActionState>();

		File[] TrajDir = trajectoryFiles.listFiles();
		File[] ProbDir = problemFiles.listFiles();

		if (TrajDir != null) 
			for (File trj : TrajDir) {

				String trajectoryPath = trj.getPath();
				String trajectoryName = FilenameUtils.getBaseName(trajectoryPath);

				String ext = FilenameUtils.getExtension(trajectoryPath); 

				if(ext.equals(Globals.TRAJECTORY_FILE_EXTENSION)) {

					LOGGER.info("Generating sequence for trajectory in " + trajectoryName);

					if (ProbDir != null) 
						for (File prb : ProbDir) {

							String problemPath = prb.getPath();
							String problemName = FilenameUtils.getBaseName(problemPath);

							if(problemName.equals(trajectoryName)) {	

								List<StateActionState> res = generateSequance(trajectoryPath);
								trajectorySequences.addAll(res);

							}
						}
				}

			}

		return trajectorySequences;
	}

	/*
	private List<StateActionState> generateSequance(String agentName, String trajectoryPath){

		LOGGER.info("Generating state action state sequence for " + trajectoryPath );

		List<StateActionState> res = new ArrayList<StateActionState>();

		String domainPath = problemFiles.getPath() + "/" + domainFileName;
		String problemPath = problemFiles.getPath() + "/" + problemFileName;
		String agentADDLPath = TEMP + "/" + problemFileName.split("\\.")[0] + ".addl";		

		Problem problem = generateProblem(agentName, domainPath, problemPath, agentADDLPath);
		currentState = problem.initState;

		try (BufferedReader br = new BufferedReader(new FileReader(trajectoryPath))) {

			String line = br.readLine();
			while (line != null) {

				StateActionState sas = processAction(problem, line);

				if(sas != null)
					res.add(sas);

				line = br.readLine();
			}

		} catch (Exception e) {
			LOGGER.info(e,e);
			return null;
		}

		deleteTempFiles();

		return res;
	}
	 */

	private List<StateActionState> generateSequance(String trajectoryPath){

		LOGGER.info("Generating state action state sequence for " + trajectoryPath );

		List<StateActionState> res = new ArrayList<StateActionState>();

		String domainPath = problemFiles.getPath() + "/" + domainFileName;
		String problemPath = problemFiles.getPath() + "/" + problemFileName;

		Problem problem = generateProblem(domainPath, problemPath);
		currentState = problem.initState;

		try (BufferedReader br = new BufferedReader(new FileReader(trajectoryPath))) {

			String line = br.readLine();
			while (line != null) {

				StateActionState sas = processAction(problem, line);

				if(sas != null)
					res.add(sas);

				line = br.readLine();
			}

		} catch (Exception e) {
			LOGGER.info(e,e);
			return null;
		}

		deleteTempFiles();

		return res;
	}


	/*
	private Problem generateProblem(String agentName, String groundedDomainPath, 
			String groundedProblemPath, String agentADDLPath) {

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
		//SASPreprocessor preprocessor = new SASPreprocessor(sasDom, addl);

		StateActionStateSASPreprocessor preprocessor = new StateActionStateSASPreprocessor(sasDom, addl);

		return preprocessor.getProblemForAgent();
	}
	 */

	private Problem generateProblem(String groundedDomainPath, String groundedProblemPath) {

		LOGGER.info("Generating problem");

		if(!runConvert()) {
			LOGGER.info("Convert failure");
			return null;
		}

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

		StateActionStateSASPreprocessor preprocessor = new StateActionStateSASPreprocessor(sasDom);

		return preprocessor.getProblemForAgent();
	}


	private boolean runConvert(){

		LOGGER.info("Converting to pddl");

		String path = problemFiles.getPath();
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

	/*
	private StateActionState processAction(Problem problem, String line) {

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
	 */

	private StateActionState processAction(Problem problem, String line) {

		LOGGER.info("Processing action " + line);

		State pre = new State(currentState);

		Action action = getActionFromPlan(problem, line);
		String actionOwner = getActionOwner(line);

		if(action != null) {

			if(action.isApplicableIn(currentState))
				action.transform(currentState);

			return new StateActionState(getStateFacts(pre), action.getSimpleLabel(), actionOwner, getStateFacts(currentState));
		}
		else
			return null;
	}

	private Set<String> getStateFacts(State state) {

		LOGGER.info("Extracting facts from state " + state);

		Set<String> res = new HashSet<String>();

		//String str = new String(state.getDomain().humanize(state.getValues()));

		String str = humanizeStateFromDomain(state.getValues());

		int start = str.indexOf('=');

		while(start != -1) {

			int end = str.indexOf(')');

			String var = str.substring(start + 1,end + 1);

			var = var.replace("(", " ");
			var = var.replace(",", "");
			var = var.replace(")", "");

			res.add(var);

			str = str.substring(end + 1, str.length());		

			start = str.indexOf('=');
		}

		return res;
	}

	private String humanizeStateFromDomain(int[] values) {

		String out = "{";

		for(int var = 0; var < values.length; ++var){
			if(var >= 0){
				out += Domain.varNames.get(var) + "=" + Domain.valNames.get(values[var]) + ",";
			}
		}

		return out + "}";
	}


	private Action getActionFromPlan(Problem problem, String actionStr) {

		LOGGER.info("Extracting action object from action " + actionStr);

		int start = actionStr.indexOf('(');
		int end = actionStr.indexOf(')');

		if(start == -1 || end == -1)
			return null;

		String label = actionStr.substring(start+1, end);

		for (Action act : problem.getAllActions()) {
			if(act.getSimpleLabel().equals(label))
				return act;
		}

		return null;
	}

	private String getActionOwner(String actionStr) {

		LOGGER.info("Extracting action owner from action " + actionStr);

		int start = actionStr.indexOf('(');
		int end = actionStr.indexOf(')');

		if(start == -1 || end == -1)
			return null;

		String label = actionStr.substring(start+1, end);

		return label.split(" ")[1];
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