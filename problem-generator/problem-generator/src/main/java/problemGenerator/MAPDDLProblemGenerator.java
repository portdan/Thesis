package problemGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import cz.agents.dimaptools.experiment.Trace;
import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.input.sas.SASParser;
import cz.agents.dimaptools.input.sas.SASPreprocessor;
import problemGenerator.RandomPlanner.RandomPlanner;
import problemGenerator.fileGenerator.FileGenerator;

public class MAPDDLProblemGenerator implements Creator {

	private final static Logger LOGGER = Logger.getLogger(MAPDDLProblemGenerator.class);

	private static final String TRANSLATOR = "./Misc/translate/translate.py";
	private static final String PREPROCESSOR = "./preprocess-runner";
	private static final String CONVERTOR = "./Misc/convert/ma-pddl/ma-to-pddl.py";
	private static final String TEMP = "./Output/temp";
	private static final String OUTPUT = "./Output";
	private static final String ROOT = "./";

	private String domainFilePath;
	private String problemFilePath;
	private String agentFilePath;
	private String sasFilePath;
	private String preprocessSASFilePath;

	private File agentFile;
	private File sasFile;
	private File preprocesSASFile;

	private int numOfExpands;

	private int timeLimitSec;

	private SASParser sasParser;    
	private SASPreprocessor preprocessor;


	private final Set<RandomPlanner> agentSet = new LinkedHashSet<RandomPlanner>();
	private final ReceiverTable receiverTable = new DefaultReceiverTable();
	private final ExecutorService executorService = Executors.newFixedThreadPool(1);

	private final Set<Thread> threadSet = new LinkedHashSet<Thread>();

	@Override
	public void init(String[] args) {

		LOGGER.info("init start:");

		sasFilePath = "output.sas";
		preprocessSASFilePath = "output";

		if (args.length != 5 ) {
			LOGGER.fatal("provided args: " + Arrays.toString(args));
			LOGGER.fatal("Usage (from PDDL): <domain>.pddl <problem>.pddl <number of expands> <time limit (sec)>");
			System.exit(1);
		}

		if (args.length == 5){
			domainFilePath = args[1];
			problemFilePath = args[2];
			numOfExpands = Integer.parseInt(args[3]);
			timeLimitSec = Integer.parseInt(args[4]);
		}

		Trace.setFileStream("Log/trace.log");

		LOGGER.info("init end");

	}

	@Override
	public void create() {

		LOGGER.info("create start:");

		convertToPDDL();

		LOGGER.info("parse agent file");
		ADDLObject addlParser = new ADDLParser().parse(agentFile);

		translateToSAS();	

		preprocessSAS();

		LOGGER.info("parse preprocessed sas file");

		sasParser = new SASParser(preprocesSASFile);	
		preprocessor = new SASPreprocessor(sasParser.getDomain(), addlParser);	

		createEntities(addlParser);

		//runEntities();

		LOGGER.info("create end");
	}

	private void createEntities(ADDLObject addlParser) {
		LOGGER.info("create entities:");


		
		List<DIMAPWorldInterface> worlds = new ArrayList<DIMAPWorldInterface>();
		for (String agentName: addlParser.getAgentList()) {
			worlds.add(initWorld(agentName,addlParser.getAgentCount()));
			
//			
//			
//			DIMAPWorldInterface world = initWorld(agentName,addlParser.getAgentCount());
//
//			
//			
//			
//			
//			agentSet.add(new RandomPlanner(world, numOfExpands, (long)timeLimitSec*1000L));
		}
		RandomPlanner.RandomWalk(preprocessor.getGlobalInit(),null,100, worlds);	
		
	}

	private void runEntities() {
		LOGGER.info("run entities:");

		FileGenerator fg = new FileGenerator();
		if(fg.generateFile(ROOT, "walks", ".txt")){


			for (final RandomPlanner agent : agentSet) {
				agent.RandomWalk();

				fg.WriteToFile(agent.getName() +" init:");
				fg.WriteToFile(agent.initialState.toString());

				fg.WriteToFile(agent.getName() +" end:");
				fg.WriteToFile(agent.endState.toString());

			}

		}

	}


	private DIMAPWorldInterface initWorld(String agentName, int totalAgents){

		return new DefaultDIMAPWorld(
				agentName,
				initQueuedCommunicator(agentName),
				new DefaultEncoder(),
				preprocessor.getProblemForAgent(agentName),
				totalAgents
				);
	}

	private PerformerCommunicator initQueuedCommunicator(String address){
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



	private void convertToPDDL(){

		LOGGER.info("convert ma-pddl to pddl (ma-to-pddl.py)");

		String path = domainFilePath.substring(0, domainFilePath.lastIndexOf("/"));
		String domain = domainFilePath.substring(domainFilePath.lastIndexOf("/")+1, domainFilePath.indexOf("."));
		String problem = problemFilePath.substring(problemFilePath.lastIndexOf("/")+1, problemFilePath.indexOf("."));

		try {
			String cmd = CONVERTOR + " " + ROOT	+ path + " " + domain + " " + problem + " " + TEMP;
			LOGGER.info("RUN: " + cmd);
			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();
		} 
		catch (Exception e) {
			LOGGER.fatal(e,e);
			System.exit(1);
		}

		LOGGER.info("set converted domain, problem and agent file paths");

		domainFilePath = TEMP + "/" + domain + ".pddl";
		problemFilePath = TEMP + "/" + problem + ".pddl";
		agentFilePath =	TEMP + "/" + problem + ".addl";

		LOGGER.info("check the created agent file (.addl)");

		agentFile = new File(ROOT + agentFilePath);
		if (!agentFile.exists()) {
			LOGGER.fatal("Agent file " + ROOT + agentFilePath + " does not exist!");
			System.exit(1);
		}
	}

	private void translateToSAS(){

		LOGGER.info("translate pddl to .sas file (translate.py)");

		try {
			String cmd = TRANSLATOR + " " + domainFilePath + " " + problemFilePath;
			LOGGER.info("RUN: " + cmd);
			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();
		} 
		catch (Exception e) {
			LOGGER.fatal(e,e);
			System.exit(1);
		}

		LOGGER.info("check the created .sas file (.sas)");

		sasFile = new File(ROOT + sasFilePath);

		if (!sasFile.exists()) {
			LOGGER.fatal("SAS file " + ROOT + sasFilePath + " does not exist!");
			System.exit(1);
		}
	}

	private void preprocessSAS(){

		LOGGER.info("preprocess the .sas file (preprocess.exe < output.sas)");

		try {
			String cmd = PREPROCESSOR;
			LOGGER.info("RUN: " + cmd);
			Process pr = Runtime.getRuntime().exec(cmd);
			pr.waitFor();
		}
		catch (Exception e) {
			LOGGER.fatal(e,e);
			System.exit(1);
		}			

		LOGGER.info("check the preprocessed .sas file");

		preprocesSASFile = new File(ROOT + preprocessSASFilePath);

		if (!preprocesSASFile.exists()) {
			LOGGER.fatal("preprocess SAS file " + ROOT + preprocesSASFile + " does not exist!");
			System.exit(1);
		}

		LOGGER.info("move the .sas files to output folder (./Output)");

		sasFilePath = OUTPUT + "/" + sasFilePath;
		sasFile.renameTo(new File(sasFilePath));

		preprocessSASFilePath = OUTPUT + "/" + preprocessSASFilePath + "-preprocess.sas";
		preprocesSASFile.renameTo(new File(preprocessSASFilePath));
		preprocesSASFile = new File(preprocessSASFilePath);
	}
}
