package OurPlanner;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class TrajectoryLearner {

	private final static Logger LOGGER = Logger.getLogger(TrajectoryLearner.class);

	private String agentName = "";
	private String groundedFolder = "";
	private File trajectoriesFile = null;
	private String domainFileName = "";
	private String problemFileName = "";

	Map<String,List<StateActionState>> trajectorySequences = new HashMap<String,List<StateActionState>>();

	public TrajectoryLearner(String agentName, String groundedFolder, File trajectoriesFile,
			String domainFileName, String problemFileName) {

		LOGGER.info("TrajectoryLearner constructor");

		this.agentName = agentName;
		this.groundedFolder = groundedFolder;
		this.trajectoriesFile = trajectoriesFile;
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;

		logInput();

		generateSequences();
	}

	private void generateSequences() {

		LOGGER.info("Generating sequences");

		StateActionStateSequencer sequencer = new StateActionStateSequencer(agentName, groundedFolder, domainFileName,problemFileName);		

		File[] directoryListing = trajectoriesFile.listFiles();

		if (directoryListing != null) 
			for (File child : directoryListing) {

				String trajectoryPath = child.getPath();
				String trajectoryName = FilenameUtils.getBaseName(trajectoryPath);
				
				String ext = FilenameUtils.getExtension(trajectoryPath); 

				if(ext.equals(Globals.TRAJECTORY_FILE_EXTENSION)) {

					LOGGER.info("Generating sequence for trajectory in " + trajectoryName);

					List<StateActionState> res = sequencer.generateSequance(trajectoryPath);
					trajectorySequences.put(trajectoryName, res);
				}

			}
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentName: " + agentName);
		LOGGER.info("groundedFolder: " + groundedFolder);
		LOGGER.info("trajectoriesFile: " + trajectoriesFile);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
	}

	public boolean learn() {
		// TODO Auto-generated method stub
		return false;
	}

}
