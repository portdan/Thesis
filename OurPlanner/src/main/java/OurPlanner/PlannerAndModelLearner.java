package OurPlanner;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import Utils.FileCompare;
import Utils.FileDeleter;

public class PlannerAndModelLearner {

	private final static Logger LOGGER = Logger.getLogger(PlannerAndModelLearner.class);

	private static final String CLOSED_DIR_PATH = Globals.OUTPUT_CLOSED_PATH;

	private	Random rnd = new Random();

	private String domainFileName = "";
	private String problemFileName = "";

	private String agentName = "";
	private List<String> agentList = null;

	private Map<Long, List<String>> fileLengthToFileName= new HashMap<Long, List<String>>();

	public PlannerAndModelLearner(String agentName, List<String> agentList, String domainFileName,
			String problemFileName) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("PlannerAndModelLearner constructor");

		this.agentName = agentName;
		this.agentList = agentList;
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;

		logInput();

		clearSearchDirs();
	}

	private void populateLengthToFileNameMap() {
		File closed = new File(CLOSED_DIR_PATH);
		File[] closedFiles = closed.listFiles();

		for (File file : closedFiles) {

			List<String> r = fileLengthToFileName.get(file.length());

			if(r==null) {
				r = new ArrayList<String>();
				fileLengthToFileName.put(file.length(),r );
			}
			r.add(file.getName());
		}
	}

	private void clearSearchDirs() {

		File temp = new File(Globals.OUTPUT_EXPANDED_SAFE_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting extended safe models");

			try {
				FileUtils.cleanDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
			}
		}

		temp = new File(Globals.OUTPUT_EXPANDED_UNSAFE_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting extended unsafe models");

			try {
				FileUtils.cleanDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
			}
		}

		temp = new File(Globals.OUTPUT_OPEN_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting open models folder");

			try {
				FileUtils.cleanDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
			}
		}

		temp = new File(Globals.OUTPUT_CLOSED_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting closed models folder");

			try {
				FileUtils.cleanDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
			}
		}
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentName: " + agentName);
		LOGGER.info("agentList: " + agentList);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
	}

	private boolean initializeOpen() {

		LOGGER.info("Initielize Open list with safe model");

		String safeModelFilePath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
		String openDirPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		File srcFile = new File(safeModelFilePath);
		File dstFile = new File(openDirPath);

		try {
			FileUtils.copyFileToDirectory(srcFile, dstFile);
		} catch (IOException e) {
			LOGGER.fatal(e, e);
			return false;
		}

		return true;

	}

	private String popOpen() {

		LOGGER.info("pop model from Open list");

		String openDirPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		File openDir = new File(openDirPath);

		String[] openModels = openDir.list();

		if(openModels!=null) {

			int randomModelIndex = rnd.nextInt(openModels.length);

			return openModels[randomModelIndex];
		}

		return null;
	}

	private boolean removeAllThatAppearInClosed(String dirExpanded) {

		LOGGER.info("Get all Expanded models that are not in Closed");

		File expanded = new File(dirExpanded);
		File[] expandedFiles = expanded.listFiles();

		if(expandedFiles == null)
			return false;

		for (File expandedFile : expandedFiles) {	

			List<String> files = fileLengthToFileName.get(expandedFile.length());

			if(files!=null) {

				for (String cls : files) {

					File closedFile = new File(CLOSED_DIR_PATH + "/" + cls);

					if(FileCompare.compareFiles(expandedFile, closedFile)) {
						expandedFile.delete();
						break;
					}
				}
			}
		}

		return true;
	}

	private List<String> plan(String modelFilePath) {

		LOGGER.info("Planning for leader agent");

		String agentProblemPath = Globals.INPUT_LOCAL_VIEW_PATH + "/" + agentName + "/" + problemFileName;
		String agentADDLPath = Globals.OUTPUT_TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";
		
		String heuristic = "saFF-glcl";
		int recursionLevel = -1;
		double timeLimitMin = 1;

		MADLAPlanner planner = new MADLAPlanner(modelFilePath, agentProblemPath, agentADDLPath,
				heuristic, recursionLevel, timeLimitMin, agentList, agentName);

		List<String> result = planner.plan();

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;
		}

		return result;
	}
}
