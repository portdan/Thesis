package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class StateActionStateSequencer {

	private final static Logger LOGGER = Logger.getLogger(StateActionStateSequencer.class);

	private static final String SAS_FILE_PATH = Globals.SAS_OUTPUT_FILE_PATH;

	private List<String> agentList = null;

	private String domainFileName = "";
	private String problemFileName = "";
	private String sasFileName = "";
	private File problemFiles = null;
	private File trajectoryFiles = null;

	private int lastTraceLine = 0;

	public int lastTrace = 0;
	public boolean StopSequencing = false;

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


	public void setSequencingData() {
		
		LOGGER.info("Setting Sequencing Variables");

		lastTraceLine = 0;
		lastTrace = 0;
	}

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

		return trajectorySequences;
	}
	
	public List<StateActionState> generateSequencesFromSASTraces2(int numOfTracesToUse, int tracesInterval) {

		LOGGER.info("Generating sequences from SAS traces");

		List<StateActionState> trajectorySequences = new ArrayList<StateActionState>();

		File[] TrajDir = trajectoryFiles.listFiles();

		int traceNumber = 0;

		if (TrajDir != null) {

			File tracesFile = TrajDir[0];

			try(BufferedReader br = new BufferedReader(new FileReader(tracesFile))) {

				for (int i = 0; i < lastTraceLine; i++)
					br.readLine();

				for(String line; (line = br.readLine()) != null; ) {

					if(line.startsWith("StateActionState")) {
						
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
						}
						
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

		return trajectorySequences;
	}

}