package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.input.sas.SASParser;
import cz.agents.dimaptools.input.sas.SASPreprocessor;
import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;
import cz.agents.dimaptools.model.SuperState;

public class PlanVerifier {

	private final static Logger LOGGER = Logger.getLogger(PlanVerifier.class);

	private static final String TRANSLATOR = "./Scripts/translate/translate.py";
	private static final String PREPROCESSOR = "./Scripts/preprocess/preprocess-runner";
	private static final String CONVERTOR = "./Scripts/ma-pddl/ma-to-pddl.py";

	private static final String TEMP = Globals.TEMP_PATH;

	private String domainFileName;
	private String problemFileName;
	private String agentFileName;
	private String sasFileName;

	private boolean sasSolvable = false;

	protected SASPreprocessor preprocessor;

	public PlanVerifier(String domainFileName, String problemFileName, String agentFileName) {

		LOGGER.info("PlanVerifier constructor");

		sasFileName = "output";
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.agentFileName = agentFileName;

		logInput();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("sasFileName: " + sasFileName);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("agentFileName: " + agentFileName);
	}

	public boolean verifyPlan(List<String> plan, List<String> agents) {

		LOGGER.info("Verifing");

		if(!runConvert()) {
			LOGGER.info("Convert failure");
			return false;
		}

		File agentFile = new File( agentFileName);
		if (!agentFile.exists()) {
			LOGGER.info("Agent file " + agentFileName + " does not exist!");
			return false;
		}

		ADDLObject addl = new ADDLParser().parse(agentFile);

		if(domainFileName == null) domainFileName = sasFileName;
		if(problemFileName == null) problemFileName = sasFileName;

		if(!runTranslate()) {
			LOGGER.info("Translate failure");
			return false;
		}

		if (!sasSolvable) {
			LOGGER.info("Sas not Solvable. Plan not found!");
			return false;
		}

		if(!runPreprocess()) {
			LOGGER.info("Preprocess failure");
			return false;
		}

		File sasFile = new File(sasFileName);
		if (!sasFile.exists()) {
			LOGGER.info("SAS file " + sasFileName + " does not exist!");
			return false;
		}

		SASParser parser = new SASParser(sasFile);
		preprocessor = new SASPreprocessor(parser.getDomain(), addl);

		return verifyPlanForAgent(plan, agents);
	}

	private boolean verifyPlanForAgent(List<String> plan, List<String> agents) {

		LOGGER.info("Verifiing plan for agent " + agents);

		LOGGER.info("Generating problems:");

		Map<String,Problem> problems = new HashMap<String,Problem>();

		for (String agent : agents) {
			LOGGER.info("Generating problem for agent: " + agent);
			problems.put(agent, preprocessor.getProblemForAgent(agent));
		}

		State state = preprocessor.getGlobalInit();
		SuperState goalState = preprocessor.getGlobalGoal();

		for(String s : plan){
			String[] split = s.split(" ");
			String agent = split[1];

			int hash = Integer.parseInt(split[split.length-1]);

			Action a = problems.get(agent).getAction(hash);

			if(a!=null) {

				if(a.isApplicableIn(state)){
					a.transform(state);
				}
				else{
					LOGGER.info("Action " + a + " of agent " + agent + "is not applicable in " + s +"!");
					return false;
				}
			}
		}

		if(state.unifiesWith(goalState)){
			LOGGER.info("Goal is reached - plan verified!");
			return true;
		}
		else{
			LOGGER.info("Goal is not reached - plan not verified!");
			return false;
		}

	}

	private boolean runConvert(){

		LOGGER.info("Converting to pddl");

		String path = domainFileName.substring(0, domainFileName.lastIndexOf("/"));
		String domain = domainFileName.substring(domainFileName.lastIndexOf("/")+1, domainFileName.lastIndexOf("."));
		String problem = problemFileName.substring(problemFileName.lastIndexOf("/")+1, problemFileName.lastIndexOf("."));

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

		domainFileName = TEMP + "/" + domain + ".pddl";
		problemFileName = TEMP + "/" + problem + ".pddl";

		return true;
	}

	private boolean runTranslate(){

		LOGGER.info("Translating to sas");

		try {
			String cmd = TRANSLATOR + " " + domainFileName + " " + problemFileName;
			LOGGER.info("RUN: " + cmd);
			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
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
}