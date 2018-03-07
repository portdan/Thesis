package problemGenerator;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import cz.agents.alite.communication.channel.DirectCommunicationChannel.DefaultReceiverTable;
import cz.agents.alite.communication.channel.DirectCommunicationChannel.ReceiverTable;
import cz.agents.alite.creator.Creator;
import cz.agents.dimaptools.experiment.Trace;
import cz.agents.dimaptools.input.sas.SASPreprocessor;
import cz.agents.madla.planner.Planner;

public class MAPDDLProblemGenerator implements Creator {

	private final static Logger LOGGER = Logger.getLogger(MAPDDLProblemGenerator.class);

	private static final String TRANSLATOR = "./misc/translate/translate.py";
	private static final String PREPROCESSOR = "./misc/preprocess/preprocess-runner";
	private static final String CONVERTOR = "./misc/ma-pddl/ma-to-pddl.py";
	private static final String TEMP = "./misc/temp";
	private static final String OUTPUT = "./output";

	private String domainFileName;
	private String problemFileName;
	private String agentFileName;

	private int numOfExpands;

	protected SASPreprocessor preprocessor;

	@Override
	public void init(String[] args) {

		for(int i=0;i<args.length;i++){
			System.out.println(args[i]);
		}

		if (args.length != 5 ) {
			System.out.println("provided args: " + Arrays.toString(args));
			System.out.println("Usage (from PDDL): <domain>.pddl <problem>.pddl <agents>.addl <number of expands>");
			System.exit(1);
		}

		if (args.length == 5){
			domainFileName = args[1];
			problemFileName = args[2];
			agentFileName = args[3];
			numOfExpands = Integer.parseInt(args[4]);
		}

		//Trace.setFileStream("log/trace.log");
	}

	@Override
	public void create() {


	}



}
