package OurPlanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cz.agents.alite.communication.PerformerCommunicator;
import cz.agents.alite.communication.QueuedCommunicator;
import cz.agents.alite.communication.channel.CommunicationChannelException;
import cz.agents.alite.communication.channel.DirectCommunicationChannel.DefaultReceiverTable;
import cz.agents.alite.communication.channel.DirectCommunicationChannel.ReceiverTable;
import cz.agents.alite.communication.channel.DirectCommunicationChannelAsync;
import cz.agents.dimaptools.DIMAPWorldInterface;
import cz.agents.dimaptools.DefaultDIMAPWorld;
import cz.agents.dimaptools.communication.protocol.DefaultEncoder;
import cz.agents.dimaptools.experiment.DataAccumulator;
import cz.agents.dimaptools.input.addl.ADDLObject;
import cz.agents.dimaptools.input.addl.ADDLParser;
import cz.agents.dimaptools.input.sas.SASParser;
import cz.agents.dimaptools.input.sas.SASPreprocessor;
import model.IPCOutputExecutor;
import model.Planner;

public class MADLAPlanner {

	private final static Logger LOGGER = Logger.getLogger(MADLAPlanner.class);

	private static final String TRANSLATOR = "./Scripts/translate/translate.py";
	private static final String PREPROCESSOR = "./Scripts/preprocess/preprocess-runner";
	private static final String CONVERTOR = "./Scripts/ma-pddl/ma-to-pddl.py";

	private static final String TEMP = Globals.TEMP_PATH;
	private static final String OUTPUT = Globals.OUTPUT_PATH + "/out.csv";

	private final ReceiverTable receiverTable = new DefaultReceiverTable();
	private final ExecutorService executorService = Executors.newFixedThreadPool(1);

	private boolean fromSAS = true;

	private String domainFileName = "";
	private String problemFileName = "";
	private String sasFileName = "";
	private String agentFileName = "";

	private String heuristic = "";
	private int recursionLevel = -1;
	private double timeLimitMin = -1;

	private List<String> agentNames = null;

	private String planninAagentName = "";

	private boolean sasSolvable = false;

	protected SASPreprocessor preprocessor = null;

	private final Set<Planner> planners = new LinkedHashSet<Planner>();
	private final Set<Thread> threadSet = new LinkedHashSet<Thread>();

	public boolean isNotSolved = false;
	public boolean isTimeout = false;

	public MADLAPlanner(String domainFileName, String problemFileName, String agentFileName,
			String heuristic , int recursionLevel, double timeLimitMin, List<String> agentNames,
			String planninAagentName ) {

		LOGGER.info("MADLAPlanner constructor (not from .sas)");

		fromSAS = false;
		sasFileName = "output.sas";
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.agentFileName = agentFileName;
		this.heuristic = heuristic;
		this.recursionLevel = recursionLevel;
		this.timeLimitMin = timeLimitMin;
		this.agentNames = agentNames;
		this.planninAagentName = planninAagentName;

		logInput();
	}

	public MADLAPlanner(String sasFileName, String agentFileName, String heuristic ,
			int recursionLevel, double timeLimitMin, List<String> agentNames, String planninAagentName ) {

		LOGGER.info("MADLAPlanner constructor (from .sas)");

		this.sasFileName = sasFileName;
		this.agentFileName = agentFileName;
		this.heuristic = heuristic;
		this.recursionLevel = recursionLevel;
		this.timeLimitMin = timeLimitMin;
		this.agentNames = agentNames;
		this.planninAagentName = planninAagentName;

		logInput();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("sasFileName: " + sasFileName);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("agentFileName: " + agentFileName);
		LOGGER.info("heuristic: " + heuristic);
		LOGGER.info("recursionLevel: " + recursionLevel);
		LOGGER.info("timeLimitMin: " + timeLimitMin);
		LOGGER.info("agentNames: " + agentNames);
		LOGGER.info("agentName: " + planninAagentName);
	}

	public List<String> plan() {

		LOGGER.info("Starting planning");

		long startTime = System.currentTimeMillis();

		DataAccumulator.startNewAccumulator(domainFileName, problemFileName, 0, heuristic, recursionLevel);
		DataAccumulator.getAccumulator().startTimeMs = startTime;

		long tCPU = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();

		for (String agent : agentNames)
			DataAccumulator.getAccumulator().startCPUTimeMs.put(agent, tCPU);

		DataAccumulator.getAccumulator().setOutputFile(OUTPUT);

		if(!runConvert()) {
			LOGGER.info("Convert failure");
			return null;
		}

		LOGGER.info(">>> CREATION");
		LOGGER.info(">>>   sas: " + sasFileName);
		LOGGER.info(">>>   agents: " + agentFileName);

		File agentFile = new File( agentFileName);
		if (!agentFile.exists()) {
			LOGGER.info("Agent file " + agentFileName + " does not exist!");
			return null;
		}

		ADDLObject addl = new ADDLParser().parse(agentFile);

		DataAccumulator.getAccumulator().agents = addl.getAgentCount();

		if(domainFileName == null) domainFileName = sasFileName;
		if(problemFileName == null) problemFileName = sasFileName;

		if(!fromSAS){

			if(!runTranslate()) {
				LOGGER.info("Translate failure");
				return null;
			}

			if (!sasSolvable) {
				LOGGER.info("Sas not Solvable. Plan not found!");
				DataAccumulator.getAccumulator().finishTimeMs = System.currentTimeMillis();
				DataAccumulator.getAccumulator().finished = false;
				DataAccumulator.getAccumulator().planLength = -1;
				DataAccumulator.getAccumulator().planValid = false; 
				DataAccumulator.getAccumulator().writeOutput(Planner.FORCE_EXIT_AFTER_WRITE);

				isNotSolved = true;

				return null;
			}

			if(!runPreprocess()) {
				LOGGER.info("Preprocess failure");
				return null;
			}
		}

		File sasFile = new File(sasFileName);
		if (!sasFile.exists()) {
			LOGGER.info("SAS file " + sasFileName + " does not exist!");
			return null;
		}

		SASParser parser = new SASParser(sasFile);
		preprocessor = new SASPreprocessor(parser.getDomain(), addl);

		DataAccumulator.getAccumulator().startAfterPreprocessTimeMs = System.currentTimeMillis();

		createEntities(addl);

		runEntities();

		executorService.shutdown();

		try {
			executorService.awaitTermination(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			LOGGER.warn("Shutdown interrupted!");
		}

		for (final Planner planner : planners) {    
			if (planner.foundPlan != null)
				return planner.foundPlan;
		}

		for (final Planner planner : planners) {    
			if (planner.isTimeout) {
				isTimeout = true;
			}
		}

		if(isTimeout)
			DataAccumulator.getAccumulator().writeOutput(Planner.FORCE_EXIT_AFTER_WRITE);

		return null;      
	}

	private boolean runConvert(){

		LOGGER.info("Converting to pddl");

		String path = domainFileName.substring(0, domainFileName.lastIndexOf("/"));
		String domain = domainFileName.substring(domainFileName.lastIndexOf("/")+1, domainFileName.lastIndexOf("."));
		String problem = problemFileName.substring(problemFileName.lastIndexOf("/")+1, problemFileName.lastIndexOf("."));

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

		domainFileName = TEMP + "/" + domain + ".pddl";
		problemFileName = TEMP + "/" + problem + ".pddl";

		return true;
	}

	private boolean runTranslate(){

		LOGGER.info("Translating to sas");

		try {
			String cmd = TRANSLATOR + " " + domainFileName + " " + problemFileName;
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

	private void createEntities(ADDLObject addl) {
		LOGGER.info(">>> ENTITIES CREATION");

		String planOutputFileName = Globals.OUTPUT_PATH + "/out.plan";

		IPCOutputExecutor executor = new IPCOutputExecutor(planOutputFileName);

		for (String agentName: addl.getAgentList()) {

			DIMAPWorldInterface world = initWorld(agentName,addl.getAgentCount());

			planners.add(new Planner(heuristic,recursionLevel,world,executor,(long)timeLimitMin*60L*1000L));

			executor.addProblem(world.getProblem());
		}

		executor.setInitAndGoal(preprocessor.getGlobalInit(), preprocessor.getGlobalGoal());

	}

	public PerformerCommunicator initQueuedCommunicator(String address){
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

	public DIMAPWorldInterface initWorld(String agentName, int totalAgents){

		return new DefaultDIMAPWorld(
				agentName,
				initQueuedCommunicator(agentName),
				new DefaultEncoder(),
				preprocessor.getProblemForAgent(agentName),
				totalAgents
				);
	}

	@SuppressWarnings("deprecation")
	private void runEntities() {
		LOGGER.info(">>> ENTITIES RUNNING");


		final Thread mainThread = Thread.currentThread();
		final UncaughtExceptionHandler exceptionHandler = new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				LOGGER.error("Uncaught exception in agent/planner thread!", e);

				DataAccumulator.getAccumulator().finishTimeMs = DataAccumulator.getAccumulator().startTimeMs-1;
				DataAccumulator.getAccumulator().writeOutput(Planner.FORCE_EXIT_AFTER_WRITE);

				for (final Planner agent : planners) {
					agent.getWorld().getCommPerformer().performClose();
				}

				mainThread.interrupt();
			}

		};

		for (final Planner agent : planners) {

			//            final Communicator comm = initCommunicator(agent);

			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					agent.planAndExecuteFinal();
				}
			}, agent.getName());
			thread.setUncaughtExceptionHandler(exceptionHandler);
			threadSet.add(thread);
			thread.start();
		}

		try {
			for (Thread thread : threadSet) {
				thread.join();
			}
		} catch (InterruptedException e) {
			LOGGER.debug("Main thread interrupted.");
		}

		for (Thread thread : threadSet) {
			if (thread.isAlive()) {
				// TODO: refactor using a stop flag
				thread.stop();
			}
		}
	}

}
