package domainLerner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import cz.agents.dimaptools.model.State;

public class MAPDDLDomainLerner implements Creator {

	private final static Logger LOGGER = Logger.getLogger(MAPDDLDomainLerner.class);

	private static final String TRANSLATOR = "./Misc/translate/translate.py";
	private static final String PREPROCESSOR = "./preprocess-runner";
	private static final String CONVERTOR = "./Misc/convert/ma-pddl/ma-to-pddl.py";
	private static final String REMOVEDUPLICATOR = "./Misc/removeDuplicates/rmDup.py";

	private static final String TEMP = "./Output/temp";
	private static final String GEN = "./Output/gen";
	private static final String OUTPUT = "./Output";
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

	@Override
	public void init(String[] args) {

		LOGGER.info("init start:");

		sasFilePath = "output.sas";
		preprocessSASFilePath = "output";

		
		if (args.length != 3) {
			LOGGER.fatal("provided args: " + Arrays.toString(args));
			LOGGER.fatal("Usage (from PDDL): <domain>.pddl <problem>.pddl");
			//LOGGER.fatal("Usage (from PDDL): <domain>.pddl <problem>.pddl <number of expands> <time limit (sec)>");
			System.exit(1);
		}

		if (args.length == 3) {
			domainFilePath = args[1];
			oldDomainFilePath = args[1];
			problemFilePath = args[2];
			oldProblemFilePath = args[2];
			//maxNumOfExpands = Integer.parseInt(args[3]);
			//numOfProblemsToGenerate = Integer.parseInt(args[4]);
		}

		Trace.setFileStream("Log/trace.log");

		LOGGER.info("init end");

	}

	@Override
	public void create() {

		LOGGER.info("create start:");

		createEntities();

		runEntities();

		LOGGER.info("create end");
	}

	private void createEntities() {
		LOGGER.info("create entities:");
	}

	private void runEntities() {
		LOGGER.info("run entities:");
	}

}
