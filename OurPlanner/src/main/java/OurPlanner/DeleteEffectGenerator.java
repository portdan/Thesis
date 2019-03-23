package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.Domain;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;
import model.StateActionStateSASPreprocessor;
import model.SASDomain;
import model.SASParser;


public class DeleteEffectGenerator {

	private final static Logger LOGGER = Logger.getLogger(DeleteEffectGenerator.class);

	private static final String TRANSLATOR = "./Scripts/translate/translate.py";
	private static final String PREPROCESSOR = "./Scripts/preprocess/preprocess-runner";
	private static final String CONVERTOR = "./Scripts/ma-pddl/ma-to-pddl.py";

	private static final String TEMP = Globals.TEMP_PATH;
	private static final String SAS_FILE_NAME = "output.sas";

	private String domainFileName = "";
	private String problemFileName = "";
	private String convertedDomainPath = "";
	private String convertedProblemPath = "";
	private String sasFileName = "";
	private File problemFiles = null;

	private boolean sasSolvable = false;
	
	private Problem problem = null;

	public DeleteEffectGenerator(File problemFiles,String domainFileName, String problemFileName) {

		LOGGER.info("DeleteEffectGenerator constructor");

		sasFileName = SAS_FILE_NAME;

		this.problemFiles = problemFiles;
		this.domainFileName = new String(domainFileName);
		this.problemFileName = new String(problemFileName);

		logInput();
		
		String domainPath = problemFiles.getPath() + "/" + domainFileName;
		String problemPath = problemFiles.getPath() + "/" + problemFileName;

		problem = generateProblem(domainPath, problemPath);
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("sasFileName: " + sasFileName);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("problemFiles: " + problemFiles);
	}

	public Set<String> generateDeleteEffects(String actionName, Set<String> pre, Set<String> eff) {

		LOGGER.info("Generating delete effects for action " + actionName );

		Set<String> res = new HashSet<String>();		

		Action action = getActionFromName(problem, actionName);

		if (action != null) {

			for (String effFact : eff) {

				int effVal = getFactValByString(effFact);
				int effVar = getFactVarByVal(problem, effVal);

				boolean isExistsBoth = false;

				for (String preFact : pre) {

					int preVal = getFactValByString(preFact);
					int preVar = getFactVarByVal(problem, preVal);

					if(effVar == preVar) {
						res.add("not ("+ preFact +")");
						isExistsBoth = true;
					}
				}

				if(!isExistsBoth) {

					Set<Integer> deleteVals = problem.getDomain().getVariableDomains().get(effVar);

					for (int val : deleteVals) {
						res.add("not ( "+ Domain.valNames.get(val) +")");
					}
				}
			}
		}

		return res;
	}

	private int getFactValByString(String fact) {
		int val = -1;

		for (int i = 0; i < Domain.valNames.getValues().length; i++) {
			if (Domain.valNames.get(i).equals(fact)) {
				val = i;
				break;
			}
		}

		return val;
	}

	private int getFactVarByVal(Problem problem, int targetVal) {

		Map<Integer, Set<Integer>> variableDomains = problem.getDomain().getVariableDomains();

		for (int var : variableDomains.keySet())
		{
			for (int val : variableDomains.get(var))
				if(val == targetVal)
					return var;	
		}

		return -1;
	}

	private Action getActionFromName(Problem problem, String actionName) {

		LOGGER.info("Extracting action object for action " + actionName);

		for (Action act : problem.getAllActions()) {
			if(act.getSimpleLabel().equals(actionName))
				return act;
		}

		return null;
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

		convertedDomainPath = TEMP + "/" + domain + ".pddl";
		convertedProblemPath = TEMP + "/" + problem + ".pddl";

		return true;
	}

	private boolean runTranslate(){

		LOGGER.info("Translating to sas");

		try {
			String cmd = TRANSLATOR + " " + convertedDomainPath + " " + convertedProblemPath + " --ignore_unsolvable";
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


}