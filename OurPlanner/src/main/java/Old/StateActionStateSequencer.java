package Old;

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
import Model.StateActionStateSASPreprocessor;
import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.Domain;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;

import OurPlanner.*;


public class StateActionStateSequencer {

	private final static Logger LOGGER = Logger.getLogger(StateActionStateSequencer.class);

	private static final String TRANSLATOR = Globals.PYTHON_SCRIPTS_FOLDER + "/translate/translate.py";
	private static final String PREPROCESSOR = Globals.PYTHON_SCRIPTS_FOLDER + "/preprocess/preprocess-runner";
	private static final String CONVERTOR = Globals.PYTHON_SCRIPTS_FOLDER + "/ma-pddl/ma-to-pddl.py";

	private static final String SAS_FILE_PATH = Globals.SAS_OUTPUT_FILE_PATH;
	private static final String OUTPUT_FILE_NAME = Globals.PROCESSED_SAS_OUTPUT_FILE_PATH;
	private static final String TEMP_DIR_PATH = Globals.OUTPUT_TEMP_PATH;

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

	private int lastActionLine = 0;
	private String agentName = "";

	private int lastTraceLine = 0;
	
	public int lastTrace = 0;
	public boolean StopSequencing = false;

	private boolean fileEnd = false;

	Map<String, File> agentTracesFiles = new HashMap<String,File>();

	public StateActionStateSequencer(List<String> agentList, File problemFiles, String domainFileName,
			String problemFileName, File trajectoryFiles ) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("StateActionStateSequencer constructor");

		sasFileName = SAS_FILE_PATH;

		this.agentList = agentList;
		this.problemFiles = problemFiles;
		this.trajectoryFiles = trajectoryFiles;
		this.domainFileName = new String(domainFileName);
		this.problemFileName = new String(problemFileName);

		//		File[] TrajDir = trajectoryFiles.listFiles();
		//
		//		if (TrajDir != null) {
		//
		//			for (File file : TrajDir) {
		//				String fileName = FilenameUtils.getBaseName(file.getPath());
		//
		//				agentTracesFiles.put(fileName, file);				
		//			}
		//		}

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

	public void setSequencingData(String agentName) {

		lastActionLine = 0;
		fileEnd = false;

		this.agentName = agentName;
	}

	public void setSequencingData() {

		lastTraceLine = 0;
		lastTrace = 0;
		fileEnd = false;
	}

	/*
	public List<StateActionState> generateSASListForAction(int numOfTracesToUse) {

		LOGGER.info("Generating sequences from SAS traces");

		String newActionStart = "actionName - ";

		List<StateActionState> trajectorySequences = new ArrayList<StateActionState>();

		boolean traceNumberReached = false;
		boolean actionStart = false;

		int maxTracesNumber = 0;

		File tracesFile = agentTracesFiles.get(agentName);

		if(tracesFile != null) {

			try(BufferedReader br = new BufferedReader(new FileReader(tracesFile))) {

				for (int i = 0; i < lastActionLine; i++)
					br.readLine();

				for(String line; (line = br.readLine()) != null; ) {

					if(line.startsWith(newActionStart)) {

						if(actionStart)
							break;

						actionStart = true;
					}

					if(traceNumberReached==false && line.startsWith("StateActionState")) {

						StateActionState sas = new StateActionState(line);

						if(sas.traceNumber > numOfTracesToUse)
							traceNumberReached = true;
						else {
							trajectorySequences.add(sas);

							maxTracesNumber = sas.traceNumber;
						}
					}

					lastActionLine++;
				}

				if(br.read() == -1)
					fileEnd = true;

			}
			catch (Exception e) {
				trajectorySequences.clear();
				return trajectorySequences;
			}

		}

		if(TestDataAccumulator.getAccumulator().trainingSize < maxTracesNumber)
			TestDataAccumulator.getAccumulator().trainingSize = maxTracesNumber;


		return trajectorySequences;
	}

	 */

	/*
	public List<StateActionState> generateSequencesFromSASTraces(int numOfTracesToUse) {

		LOGGER.info("Generating sequences from SAS traces");

		String newTraceStart = "trace_";

		List<StateActionState> trajectorySequences = new ArrayList<StateActionState>();

		File[] TrajDir = trajectoryFiles.listFiles();

		int traceNumber = 0;

		if (TrajDir != null) {

			File tracesFile = TrajDir[0];

			try(BufferedReader br = new BufferedReader(new FileReader(tracesFile))) {

				for(String line; (line = br.readLine()) != null; ) {

					if(line.startsWith(newTraceStart)) {
						if(traceNumber == numOfTracesToUse)
							break;
						else {
							traceNumber++;
							LOGGER.info("Generating sequence for trajectory number " + traceNumber);
						}
					}

					if(line.startsWith("StateActionState")) {

						StateActionState sas = new StateActionState(line);
						trajectorySequences.add(sas);
					}
				}
			}
			catch (Exception e) {
				trajectorySequences.clear();
				return trajectorySequences;
			}

		}

		TestDataAccumulator.getAccumulator().trainingSize = traceNumber;

		return trajectorySequences;
	}
	 */

	/*
	public List<StateActionState> generateSequences() {

		LOGGER.info("Generating sequences");

		List<StateActionState> trajectorySequences = new ArrayList<StateActionState>();

		File[] TrajDir = trajectoryFiles.listFiles();

		TestDataAccumulator.getAccumulator().trainingSize = TrajDir.length;

		if (TrajDir != null)
			for (File trajectoryFolder : TrajDir) {

				File[] folder = trajectoryFolder.listFiles();

				File trajectory = folder[0];
				File problem = folder[1];

				String trajectoryPath = trajectory.getPath();
				String problemPath = problem.getPath();

				String trajectoryName = FilenameUtils.getBaseName(trajectoryPath);

				String ext = FilenameUtils.getExtension(trajectoryPath); 

				if(!ext.equals(Globals.TRAJECTORY_FILE_EXTENSION)) {
					problem = folder[0];
					trajectory = folder[1];
				}

				LOGGER.info("Generating sequence for trajectory in " + trajectoryName);

				problemPath = problem.getPath();
				trajectoryPath = trajectory.getPath();

				List<StateActionState> res = generateSequance(trajectoryPath, problemPath);
				trajectorySequences.addAll(res);

			}

		return trajectorySequences;
	}



	private List<StateActionState> generateSequance(String trajectoryPath, String problemPath){

		LOGGER.info("Generating state action state sequence for " + trajectoryPath );

		List<StateActionState> res = new ArrayList<StateActionState>();

		String domainPath = problemFiles.getPath() + "/" + domainFileName;

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

	 */



	public List<StateActionState> generateSequencesFromSASTraces(int numOfTracesToUse, int tracesInterval) {

		LOGGER.info("Generating sequences from SAS traces");

		String newTraceStart = "trace_";

		List<StateActionState> trajectorySequences = new ArrayList<StateActionState>();

		File[] TrajDir = trajectoryFiles.listFiles();

		int traceNumber = 0;

		if (TrajDir != null) {

			File tracesFile = TrajDir[0];

			try(BufferedReader br = new BufferedReader(new FileReader(tracesFile))) {

				for (int i = 0; i < lastTraceLine; i++)
					br.readLine();

				for(String line; (line = br.readLine()) != null; ) {

					if(line.startsWith(newTraceStart)) {					

						if(traceNumber >= tracesInterval) {
							break;
						}
						else if(lastTrace >= numOfTracesToUse){
							StopSequencing = true;
							break;
						}
						else {
							lastTrace++;
							traceNumber++;

							LOGGER.info("Generating sequence for trajectory number " + traceNumber);
						}
					}

					if(line.startsWith("StateActionState")) {

						StateActionState sas = new StateActionState(line);
						trajectorySequences.add(sas);
					}

					lastTraceLine++;
				}

				if(br.read() == -1)
					StopSequencing = true;
			}
			catch (Exception e) {
				trajectorySequences.clear();
				return trajectorySequences;
			}

		}

		TestDataAccumulator.getAccumulator().initialTrainingSize += traceNumber;

		return trajectorySequences;
	}


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

		/* PREPROCESS NOT NEEDED
		if(!runPreprocess()) {
			LOGGER.info("Preprocess failure");
			return null;
		}
		 */

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
			String cmd = CONVERTOR + " " + path + " " + domain + " " + problem + " " + TEMP_DIR_PATH;
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

		convertedDomainPath = TEMP_DIR_PATH + "/" + domain + ".pddl";
		convertedProblemPath = TEMP_DIR_PATH + "/" + problem + ".pddl";

		return true;
	}

	private boolean runTranslate(){

		LOGGER.info("Translating to sas");

		try {
			String cmd = TRANSLATOR + " " + convertedDomainPath + " " + convertedProblemPath + " " + SAS_FILE_PATH + " --ignore_unsolvable";

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