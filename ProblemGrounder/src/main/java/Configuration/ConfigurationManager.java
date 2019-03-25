package Configuration;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigurationManager {

	private final static Logger LOGGER = Logger.getLogger(ConfigurationManager.class);

	private static ConfigurationManager instance;

	private ObjectMapper mapper = null;
	private ProblemGrounderConfiguration currentConfiguration = null;

	private ConfigurationManager(){
		mapper = new ObjectMapper();
	}

	public static ConfigurationManager getInstance(){

		if(null == instance){
			instance = new ConfigurationManager();
		}

		return instance;

	}

	public ProblemGrounderConfiguration getCurrentConfiguration() {
		return currentConfiguration;
	}

	public boolean loadConfiguration(String configurationFilePath) {

		try {
			File confFile = new File(configurationFilePath);

			if(!confFile.exists())
				return false;

			if(confFile.isFile()) {
				currentConfiguration =  mapper.readValue(confFile, ProblemGrounderConfiguration.class);
				return true;
			}
			else if(confFile.isDirectory()) {

				File[] configDir = confFile.listFiles(new FilenameFilter() { 
					public boolean accept(File dir, String filename)
					{ return filename.endsWith(".json"); }
				} );

				currentConfiguration =  mapper.readValue(configDir[0], ProblemGrounderConfiguration.class);

				return true;
			}
			else
				return false;

		}catch (Exception e) {
			LOGGER.fatal(e,e);
			currentConfiguration = null;
			return false;
		}
	}

	public boolean writeConfiguration(String configurationFilePath) {

		try {

			File confFile = new File(configurationFilePath);

			if(!confFile.exists())
				return false;

			if(confFile.isFile()) {
				mapper.writeValue(confFile, currentConfiguration);
				return true;
			}
			else if(confFile.isDirectory()) {

				File[] configDir = confFile.listFiles(new FilenameFilter() { 
					public boolean accept(File dir, String filename)
					{ return filename.endsWith(".json"); }
				} );

				mapper.writeValue(configDir[0], currentConfiguration);
				return true;
			}
			else
				return false;

		}catch (Exception e) {
			LOGGER.fatal(e,e);
			currentConfiguration = null;
			return false;
		}
	}
}