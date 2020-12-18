package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import Model.*;

import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.model.*;

public class VariableValueExtractor {

	private final static Logger LOGGER = Logger.getLogger(VariableValueExtractor.class);

	private static final String TRANSLATOR = Globals.PYTHON_SCRIPTS_FOLDER + "/translate/translate.py";
	private static final String PREPROCESSOR = Globals.PYTHON_SCRIPTS_FOLDER + "/preprocess/preprocess-runner";
	private static final String CONVERTOR = Globals.PYTHON_SCRIPTS_FOLDER + "/ma-pddl/ma-to-pddl.py";

	private static final String OUTPUT_FILE_NAME = Globals.PROCESSED_SAS_OUTPUT_FILE_PATH;
	private static final String SAS_FILE_PATH = Globals.SAS_OUTPUT_FILE_PATH;
	private static final String TEMP_DIR_PATH = Globals.OUTPUT_TEMP_PATH;

	private String domainFileName = "";
	private String problemFileName = "";
	private String problemFilesPath = "";

	private boolean sasSolvable = false;

	public VariableValueExtractor(String domainFileName,String problemFileName, String problemFilesPath) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("VariableValueExtractor constructor");

		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.problemFilesPath = problemFilesPath;

		logInput();

		if(!preparePDDLGrounded()) {
			LOGGER.info("prepare PDDls failure");
			return;
		}

		if(!readProblemPDDLGrounded()) {
			LOGGER.info("read PDDls failure");
			return;
		}
	}



	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("problemFilesPath: " + problemFilesPath);
	}

	private boolean preparePDDLGrounded() {

		LOGGER.info("Preparing pddl grounded problem");

		String groundedDomainPath = problemFilesPath + "/" + domainFileName;
		String groundedProblemPath = problemFilesPath + "/" + problemFileName;
		String outputFolder = TEMP_DIR_PATH;

		LOGGER.info("Generating problem");

		if(!runConvert(groundedDomainPath,groundedProblemPath,outputFolder)) {
			LOGGER.info("Convert failure!");
			return false;
		}

		if(!isConversionSuccessful(outputFolder)){
			return false;
		}

		return true;
	}

	private boolean isConversionSuccessful(String outputFolder) {

		LOGGER.info("Checking conversion");

		String newADDLPath = outputFolder + "/" + problemFileName.split("\\.")[0] + ".addl";		
		String newDomain = outputFolder + "/" + domainFileName;		
		String newProblem = outputFolder + "/" + domainFileName;

		if (!new File(newADDLPath).exists()) {
			LOGGER.info("File " + newADDLPath + " does not exist!");
			return false;
		}

		if (!new File(newDomain).exists()) {
			LOGGER.info("File " + newDomain + " does not exist!");
			return false;
		}

		if (!new File(newProblem).exists()) {
			LOGGER.info("File " + newProblem + " does not exist!");
			return false;
		}

		return true;
	}

	private boolean readProblemPDDLGrounded() {

		LOGGER.info("Reading grounded pddl problem");

		String ProblemPath = TEMP_DIR_PATH + "/" + problemFileName;
		String fileStr = "";

		try {
			fileStr = FileUtils.readFileToString(new File(ProblemPath),Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return false;
		}

		if(fileStr.isEmpty()) {
			LOGGER.info("PDDL " + ProblemPath + " is empty!");
			return false;
		}

		return true;
	}

	private StateActionStateSASPreprocessor generatePreprocessor(String domainPath,
			String problemPath, String agentADDLPath) {

		LOGGER.info("Generating SAS Preprocessor");

		File agentFile = new File(agentADDLPath);
		if (!agentFile.exists()) {
			LOGGER.info("Agent file " + agentADDLPath + " does not exist!");
			return null;
		}

		ADDLObject addl = new ADDLParser().parse(agentFile);

		if(domainFileName == null) domainFileName = SAS_FILE_PATH;
		if(problemFileName == null) problemFileName = SAS_FILE_PATH;

		if(!runTranslate(domainPath, problemPath)) {
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

		File sasFile = new File(SAS_FILE_PATH);
		if (!sasFile.exists()) {
			LOGGER.info("SAS file " + SAS_FILE_PATH + " does not exist!");
			return null;
		}

		SASParser parser = new SASParser(sasFile);
		SASDomain sasDom = parser.getDomain();
		StateActionStateSASPreprocessor preprocessor = new StateActionStateSASPreprocessor(sasDom);

		return preprocessor;
	}

	private boolean runConvert(String groundedDomainPath, String groundedProblemPath, String outputFolder){

		LOGGER.info("Converting to pddl");

		String path = groundedDomainPath.substring(0, groundedDomainPath.lastIndexOf("/"));
		String domain = groundedDomainPath.substring(groundedDomainPath.lastIndexOf("/")+1, groundedDomainPath.lastIndexOf("."));
		String problem = groundedProblemPath.substring(groundedProblemPath.lastIndexOf("/")+1, groundedProblemPath.lastIndexOf("."));

		try {
			String cmd = CONVERTOR + " " + path + " " + domain + " " + problem + " " + outputFolder;
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

		return true;
	}

	private boolean runTranslate(String domainPath, String problemPath){

		LOGGER.info("Translating to sas");

		try {
			String cmd = TRANSLATOR + " " + domainPath + " " + problemPath + " " + SAS_FILE_PATH + " --ignore_unsolvable";			

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
		//			e.printStackTrace();
		//		} catch (InterruptedException e) {
		//			e.printStackTrace();
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
			LOGGER.info("SAS file " + SAS_FILE_PATH + " does not exist!");
			return false;
		} catch (IOException e) {
			LOGGER.info("SAS file " + SAS_FILE_PATH + " bad!");
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

	public Map<String,Set<String>> getMapping(boolean format) {

		LOGGER.info("Getting mapping");

		String domainPath = TEMP_DIR_PATH + "/" + domainFileName;
		String problemPath = TEMP_DIR_PATH + "/" + problemFileName;
		String agentADDLPath = TEMP_DIR_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";	

		StateActionStateSASPreprocessor preprocessor = generatePreprocessor(domainPath, problemPath, agentADDLPath);

		if(preprocessor == null) {
			LOGGER.info("Mapping cannot be generated!");
			return null;
		}

		Map<String,Set<String>> agentVarVals = preprocessor.getAllVarVals();

		Map<String,Set<String>> res = new HashMap<String,Set<String>>();

		for (Entry<String, Set<String>> entry : agentVarVals.entrySet())  {
			String key = entry.getKey();
			Set<String> value = entry.getValue();

			if(format)
				value = formatFacts(value);

			res.put(key, new HashSet<String>(value));
		}

		return res;
	}

	private Set<String> formatFacts(Set<String> facts) {

		LOGGER.info("Formatting facts");

		Set<String> formatted = new HashSet<String>();

		for (String fact : facts) {

			int startIndex = 0;
			int endIndex = fact.length();

			boolean isNegated = false;
			String formattedFact = fact;

			if(formattedFact.startsWith("not")) {
				isNegated = !isNegated;
				startIndex = formattedFact.indexOf('(');
				endIndex = formattedFact.lastIndexOf(')');			
				formattedFact = formattedFact.substring(startIndex+1,endIndex);
			}

			if(formattedFact.startsWith(Globals.NEGATED_KEYWORD)) {
				isNegated = !isNegated;
				formattedFact = formattedFact.replace(Globals.NEGATED_KEYWORD, "");
			}

			formattedFact = formattedFact.replace("(", " ");
			formattedFact = formattedFact.replace(",", "");
			formattedFact = formattedFact.replace(")", "");

			formattedFact = formattedFact.trim();

			formattedFact = '(' + formattedFact + ')';

			if(formattedFact.startsWith(Globals.NONE_KEYWORD)) {
				//formatted.addAll(formatNONEFact(formattedFact, isNegated));
				//formatted.add(formattedFact);
			}
			else {
				if (isNegated)
					formattedFact = "(not " + formattedFact + ")";

				formatted.add(formattedFact);
			}
		}

		return formatted;
	}


}