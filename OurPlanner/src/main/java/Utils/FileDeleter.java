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

		File temp = new File(Globals.OUTPUT_TEMP_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'temp' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				return false;
			}
		}

		File output = new File(Globals.PROCESSED_SAS_OUTPUT_FILE_PATH);		
		if(output.exists()) {
			LOGGER.info("Deleting " + Globals.PROCESSED_SAS_OUTPUT_FILE_PATH + " file");
			output.delete();
		}

		File outputSAS = new File(Globals.SAS_OUTPUT_FILE_PATH);		
		if(outputSAS.exists()) {
			LOGGER.info("Deleting "+ Globals.SAS_OUTPUT_FILE_PATH +" file");
			outputSAS.delete();
		}

		return true;
	}

	public static boolean deleteLearnedFiles() {

		LOGGER.info("Deleting learned files");

		File temp = new File(Globals.OUTPUT_SAFE_MODEL_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'learned' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				return false;
			}
		}

		temp = new File(Globals.OUTPUT_UNSAFE_MODEL_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'learned' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				return false;
			}
		}

		return true;
	}

	public static boolean deleteOutputFiles() {

		LOGGER.info("Deleting output files");

		File temp = new File(Globals.OUTPUT_PATH);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'output' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				return false;
			}
		}

		return true;
	}


	public static void deleteSASfiles() {

		LOGGER.info("Deleting sas files");

		File output = new File(Globals.PROCESSED_SAS_OUTPUT_FILE_PATH);		
		if(output.exists()) {
			LOGGER.info("Deleting " + Globals.PROCESSED_SAS_OUTPUT_FILE_PATH + " file");
			output.delete();
		}

		File outputSAS = new File(Globals.SAS_OUTPUT_FILE_PATH);		
		if(outputSAS.exists()) {
			LOGGER.info("Deleting "+ Globals.SAS_OUTPUT_FILE_PATH +" file");
			outputSAS.delete();
		}
	}
}
