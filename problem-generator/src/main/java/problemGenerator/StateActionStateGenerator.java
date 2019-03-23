package problemGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import cz.agents.alite.creator.Creator;
import cz.agents.dimaptools.experiment.Trace;
import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.input.sas.SASParser;
import cz.agents.dimaptools.input.sas.SASPreprocessor;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;
import problemGenerator.FileGenerator.SASGenerator;
import problemGenerator.RandomWalker.StateActionStateRandomWalker;

public class StateActionStateGenerator implements Creator {

	private final static Logger LOGGER = Logger.getLogger(StateActionStateGenerator.class);

	private static final String TRANSLATOR = "./Misc/translate/translate.py";
	private static final String CONVERTOR = "./Misc/convert/ma-pddl/ma-to-pddl.py";

	private static final String TEMP = "./Output/temp";
	private static final String OUTPUT_TRACES = "./Output/traces";
	private static final String OUTPUT_PROBLEMS = "./Output/problems";
	private static final String ROOT = "./";

	private String domainFilePath;
	private String problemFilePath;
	private String oldDomainFilePath;
	private String oldProblemFilePath;
	private String agentFilePath;
	private String sasFilePath;
	private String preprocessSASFilePath;

	private String domainFileName;
	private String problemFileName;

	private File agentFile;
	private File sasFile;
	private File preprocesSASFile;

	private int maxNumOfExpands;
	private int numOfProblemsToGenerate;

	private ADDLObject addlParser;

	private SASParser sasParser;
	private SASPreprocessor preprocessor;

	private final List<Problem> problems = new ArrayList<Problem>();

	private Map<String, List<StateActionState>> newStateActionStates = new HashMap<String, List<StateActionState>>();

	@Override
	public void init(String[] args) {

		LOGGER.info("init start:");

		sasFilePath = "output.sas";
		preprocessSASFilePath = "output";

		if (args.length != 5) {
			LOGGER.fatal("provided args: " + Arrays.toString(args));
			LOGGER.fatal("Usage (from PDDL): <domain>.pddl <problem>.pddl <number of expands> <time limit (sec)>");
			System.exit(1);
		}

		if (args.length == 5) {
			domainFilePath = args[1];
			oldDomainFilePath = args[1];
			problemFilePath = args[2];
			oldProblemFilePath = args[2];
			maxNumOfExpands = Integer.parseInt(args[3]);
			numOfProblemsToGenerate = Integer.parseInt(args[4]);
		}

		Trace.setFileStream("Log/trace.log");

		LOGGER.info("init end");

	}

	@Override
	public void create() {

		LOGGER.info("create start:");

		convertToPDDL();

		LOGGER.info("parse agent file");
		addlParser = new ADDLParser().parse(agentFile);

		translateToSAS();

		/* preprocessing the sas might remove some of the variables
		preprocessSAS();

		LOGGER.info("parse preprocessed sas file");

		sasParser = new SASParser(preprocesSASFile);

		preprocessor = new SASPreprocessor(sasParser.getDomain(), addlParser);
		 */

		LOGGER.info("parse sas file");

		sasParser = new SASParser(sasFile);

		preprocessor = new SASPreprocessor(sasParser.getDomain(), addlParser);

		createEntities(addlParser);

		runEntities();

		LOGGER.info("create end");
	}

	private void createEntities(ADDLObject addlParser) {
		LOGGER.info("create entities:");

		for (String agentName : addlParser.getAgentList()) {
			problems.add(preprocessor.getProblemForAgent(agentName));
		}
	}

	private void runEntities() {
		LOGGER.info("run entities:");

		SASGenerator sasGenerator = new SASGenerator();

		String TracesFolder = OUTPUT_TRACES + "/" + problemFileName;

		sasGenerator.generateFile(TracesFolder,problemFileName);

		// generate problems
		for (int i = 0; i < numOfProblemsToGenerate; i++) {

			List<StateActionState> sasList = new ArrayList<StateActionState>();
			// perform random walk
			State endState = StateActionStateRandomWalker.RandomWalk(sasList, preprocessor.getGlobalInit(),
					preprocessor.getGlobalGoal(), maxNumOfExpands, problems);

			String humenized = endState.getDomain().humanize(endState.getValues());

			newStateActionStates.put(humenized, sasList);
		}

		int problemCounter = 1;

		Iterator<Entry<String, List<StateActionState>>> it = newStateActionStates.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, List<StateActionState>> statePlanPair = (Entry<String, List<StateActionState>>)it.next();

			List<StateActionState> sasList = statePlanPair.getValue();

			// append new sasList
			sasGenerator.appendSASList(sasList, problemCounter);

			problemCounter++;

		}

		sasGenerator.close();

		delelteTemporaryFiles();
	}


	private void delelteTemporaryFiles() {

		LOGGER.info("Deleting temporary files");

		File temp = new File(TEMP);		
		if(temp.exists()) {
			LOGGER.info("Deleting 'temp' folder");

			try {
				FileUtils.deleteDirectory(temp);
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				System.exit(1);
			}
		}
	}

	private void convertToPDDL() {

		LOGGER.info("convert ma-pddl to pddl (ma-to-pddl.py)");

		String path = domainFilePath.substring(0, domainFilePath.lastIndexOf("/"));
		domainFileName = domainFilePath.substring(domainFilePath.lastIndexOf("/") + 1, domainFilePath.indexOf("."));
		problemFileName = problemFilePath.substring(problemFilePath.lastIndexOf("/") + 1, problemFilePath.indexOf("."));

		try {
			String cmd = CONVERTOR + " " + ROOT + path + " " + domainFileName + " " + problemFileName + " " + TEMP;
			LOGGER.info("RUN: " + cmd);
			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();
		} catch (Exception e) {
			LOGGER.fatal(e, e);
			System.exit(1);
		}

		LOGGER.info("set converted domain, problem and agent file paths");

		domainFilePath = TEMP + "/" + domainFileName + ".pddl";
		problemFilePath = TEMP + "/" + problemFileName + ".pddl";
		agentFilePath = TEMP + "/" + problemFileName + ".addl";

		LOGGER.info("check the created agent file (.addl)");

		agentFile = new File(ROOT + agentFilePath);
		if (!agentFile.exists()) {
			LOGGER.fatal("Agent file " + ROOT + agentFilePath + " does not exist!");
			System.exit(1);
		}
	}

	private void translateToSAS() {

		LOGGER.info("translate pddl to .sas file (translate.py)");

		try {
			String cmd = TRANSLATOR + " " + domainFilePath + " " + problemFilePath;
			LOGGER.info("RUN: " + cmd);
			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();
		} catch (Exception e) {
			LOGGER.fatal(e, e);
			System.exit(1);
		}

		LOGGER.info("check the created .sas file (.sas)");

		sasFile = new File(ROOT + sasFilePath);

		if (!sasFile.exists()) {
			LOGGER.fatal("SAS file " + ROOT + sasFilePath + " does not exist!");
			System.exit(1);
		}
	}
}
