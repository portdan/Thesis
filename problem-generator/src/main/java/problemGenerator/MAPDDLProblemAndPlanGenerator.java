package problemGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import cz.agents.alite.communication.PerformerCommunicator;
import cz.agents.alite.communication.QueuedCommunicator;
import cz.agents.alite.communication.channel.CommunicationChannelException;
import cz.agents.alite.communication.channel.DirectCommunicationChannelAsync;
import cz.agents.alite.communication.channel.DirectCommunicationChannel.DefaultReceiverTable;
import cz.agents.alite.communication.channel.DirectCommunicationChannel.ReceiverTable;
import cz.agents.alite.creator.Creator;
import cz.agents.dimaptools.DIMAPWorldInterface;
import cz.agents.dimaptools.DefaultDIMAPWorld;
import cz.agents.dimaptools.communication.protocol.DefaultEncoder;
import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.input.sas.SASParser;
import cz.agents.dimaptools.input.sas.SASPreprocessor;
import cz.agents.dimaptools.model.State;
import problemGenerator.FileGenerator.PDDLGenerator;
import problemGenerator.FileGenerator.PlanGenerator;
import problemGenerator.RandomWalker.RandomWalker;

public class MAPDDLProblemAndPlanGenerator implements Creator {

	private final static Logger LOGGER = Logger.getLogger(MAPDDLProblemAndPlanGenerator.class);

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

	private final ReceiverTable receiverTable = new DefaultReceiverTable();
	private final ExecutorService executorService = Executors.newFixedThreadPool(1);

	private final List<DIMAPWorldInterface> worlds = new ArrayList<DIMAPWorldInterface>();

	private Map<String, List<String>> newStateAndPlan = new HashMap<String, List<String>>();

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

		//Trace.setFileStream("Log/trace.log");
		LOGGER.setLevel(Level.INFO);

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

	/*
	 * private void createEntities(ADDLObject addlParser) {
	 * LOGGER.info("create entities:");
	 * 
	 * for (String agentName: addlParser.getAgentList()) {
	 * 
	 * DIMAPWorldInterface world = initWorld(agentName,addlParser.getAgentCount());
	 * 
	 * agentSet.add(new RandomPlanner(world, numOfExpands,
	 * (long)timeLimitSec*1000L)); } }
	 */

	private void createEntities(ADDLObject addlParser) {
		LOGGER.info("create entities:");

		for (String agentName : addlParser.getAgentList()) {
			worlds.add(initWorld(agentName, addlParser.getAgentCount()));
		}
	}

	/*
	 * private void runEntities() { LOGGER.info("run entities:");
	 * 
	 * FileGenerator fg = new FileGenerator(); if(fg.generateFile(ROOT, "walks",
	 * ".txt")){
	 * 
	 * 
	 * for (final RandomPlanner agent : agentSet) { agent.RandomWalk();
	 * 
	 * fg.WriteToFile(agent.getName() +" init:");
	 * fg.WriteToFile(agent.initialState.toString());
	 * 
	 * fg.WriteToFile(agent.getName() +" end:");
	 * fg.WriteToFile(agent.endState.toString());
	 * 
	 * } } }
	 */

	private void runEntities() {
		LOGGER.info("run entities:");

		PDDLGenerator pddlGenerator = new PDDLGenerator();
		PlanGenerator planGenerator = new PlanGenerator();

		int problemCounter = 0;

		// get old problem text
		String problemText = null;
		try {
			problemText = new String(Files.readAllBytes(Paths.get(oldProblemFilePath)));
		} catch (IOException e) {
			LOGGER.fatal(e, e);
			System.exit(1);
		}

		// generate problems
		for (int i = 0; i < numOfProblemsToGenerate; i++) {

			List<String> plan = new ArrayList<String>();

			// perform random walk
			State endState = RandomWalker.RandomWalk(plan, preprocessor.getGlobalInit(), preprocessor.getGlobalGoal(), maxNumOfExpands, worlds);
			String humenized = endState.getDomain().humanize(endState.getValues());

			newStateAndPlan.put(humenized, plan);
		}

		Iterator<Entry<String, List<String>>> it = newStateAndPlan.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, List<String>> statePlanPair = (Entry<String, List<String>>)it.next();

			String TracesFolder = OUTPUT_TRACES + "/" + problemFileName + "_" + problemCounter;

			String newProblemFileName = problemFileName + "_" + problemCounter;
			String newPlanFileName = "out";			

			try {
				//  problem file created
				pddlGenerator.generateFile(TracesFolder,newProblemFileName);
				// generate new problem
				pddlGenerator.generateRandomProblem(problemText,newProblemFileName, statePlanPair.getKey());		

				//  plan file created
				planGenerator.generateFile(TracesFolder,newPlanFileName);
				// generate plan
				planGenerator.generatePlan(statePlanPair.getValue());		

				//  problem file created
				pddlGenerator.generateFile(OUTPUT_PROBLEMS,newProblemFileName);
				// generate new problem
				pddlGenerator.generateRandomProblem(problemText,newProblemFileName, statePlanPair.getKey());		

			}
			catch (IOException e) {
				e.printStackTrace();
			}

			problemCounter++;
		}

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

	private DIMAPWorldInterface initWorld(String agentName, int totalAgents) {

		return new DefaultDIMAPWorld(agentName, initQueuedCommunicator(agentName), new DefaultEncoder(),
				preprocessor.getProblemForAgent(agentName), totalAgents);
	}

	private PerformerCommunicator initQueuedCommunicator(String address) {
		QueuedCommunicator communicator = new QueuedCommunicator(address);
		try {

			communicator.handleMessageClass(Object.class);

			communicator.addChannel(new DirectCommunicationChannelAsync(communicator, receiverTable, executorService));
		} catch (CommunicationChannelException e) {
			LOGGER.fatal("Communication channel creation error!", e);
			System.exit(1);
		}

		return communicator;
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
