package Utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import OurPlanner.Globals;

public class FileDeleter {

	public final static Logger LOGGER = Logger.getLogger(FileDeleter.class);

	public static boolean deleteTempFiles() {

		LOGGER.info("Deleting temporary files");

		deleteFile(Globals.PROCESSED_SAS_OUTPUT_FILE_PATH);

		deleteFile(Globals.SAS_OUTPUT_FILE_PATH);

		return cleanDirectory(Globals.OUTPUT_TEMP_PATH, true);
	}

	public static boolean deleteLearnedFiles() {

		LOGGER.info("Deleting learned files");

		return cleanDirectory(Globals.OUTPUT_SAFE_MODEL_PATH, true) && cleanDirectory(Globals.OUTPUT_UNSAFE_MODEL_PATH, true);

	}

	public static boolean deleteOutputFiles() {

		LOGGER.info("Deleting output files");

		return cleanDirectory(Globals.OUTPUT_PATH, true);
	}

	public static boolean cleanDirectory(String folderPath, boolean delete) {

		LOGGER.info("Clearing files in folder " + folderPath);

		File file = new File(folderPath);		
		if(file.exists()) {
			try {
				if(delete)
					FileUtils.deleteDirectory(file);
				else
					FileUtils.cleanDirectory(file);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				return false;
			}
		}

		return true;
	}

	public static void deleteSASfiles() {

		LOGGER.info("Deleting sas files");

		deleteFile(Globals.PROCESSED_SAS_OUTPUT_FILE_PATH);

		deleteFile(Globals.SAS_OUTPUT_FILE_PATH);
	}

	public static void deleteFile(String filePath) {

		LOGGER.info("Deleting file: " + filePath);

		File output = new File(filePath);		
		if(output.exists()) {
			LOGGER.info("Deleting file: " + filePath);
			output.delete();
		}
	}
}
