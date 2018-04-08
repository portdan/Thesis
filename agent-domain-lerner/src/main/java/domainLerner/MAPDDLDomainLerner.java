package domainLerner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;

import cz.agents.alite.creator.Creator;

import cz.agents.dimaptools.experiment.Trace;

public class MAPDDLDomainLerner implements Creator {

	private final static Logger LOGGER = Logger.getLogger(MAPDDLDomainLerner.class);

	private static final String MA_TO_PDDL_CONVERTOR = "./Misc/converters/unfactoredMAPDDL-to-PDDL.py";
	private static final String PDDL_TO_MA_CONVERTOR = "./Misc/converters/PDDL-to-unfactoredMAPDDL.py";
	private static final String MA_TO_AGENT_FILE = "./Misc/extractAgentFile/unfactoredMAPDDL-extract-agent-file.py";

	private static final String MA_TO_PDDL_OUTPUT = "./Output/to-pddl";
	private static final String PDDL_TO_MA_OUTPUT = "./Output/to-ma";

	private String domainAndProblemDirectoryPath;

	private String domainName;
	private String problemName;

	private String agentFilePath;

	private File agentFile;

	private String PickedAagent;
	private String PickedAagentType;

	@Override
	public void init(String[] args) {

		LOGGER.info("init start:");

		if (args.length != 4) {
			LOGGER.fatal("provided args: " + Arrays.toString(args));
			LOGGER.fatal("Usage: <path to .pddl files> <domain name> <problem name>");
			System.exit(1);
		}

		if (args.length == 4) {

			domainAndProblemDirectoryPath = args[1];
			domainName = args[2];
			problemName = args[3];

		}

		Trace.setFileStream("Log/trace.log");

		LOGGER.info("init end");

	}

	@Override
	public void create() {

		LOGGER.info("create start:");

		createAgentsFile();

		pickRandomAgent();

		removeAllOtherAgents();

		createAgentDomain();

		createAgentProblem();

		LOGGER.info("create end");
	}

	private void createAgentProblem() {
		LOGGER.info("create agent problem:");

		LOGGER.info("create agent problem end");
	}

	private void createAgentDomain() {
		LOGGER.info("create agent domain start:");

		LOGGER.info("create agent domian end");
	}

	private void createAgentsFile() {

		LOGGER.info("convert to PDDL start:");

		try {

			String cmd = MA_TO_AGENT_FILE + " " + domainAndProblemDirectoryPath + " " + domainName + " " + problemName
					+ " " + MA_TO_PDDL_OUTPUT;

			LOGGER.info("RUN: " + cmd);

			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();

		} catch (Exception e) {
			LOGGER.fatal(e, e);
			System.exit(1);
		}

		LOGGER.info("convert to PDDL end");
	}

	private void pickRandomAgent() {

		LOGGER.info("pick random agent start:");

		agentFilePath = MA_TO_PDDL_OUTPUT + "/" + problemName + ".agents";

		LOGGER.info("check the created agents file (.agents)");

		agentFile = new File(agentFilePath);
		if (!agentFile.exists()) {
			LOGGER.fatal("Agent file " + agentFilePath + " does not exist!");
			System.exit(1);
		}

		Random rand = new Random(2); // fixed seed
		// Random rand = new Random(); // no seed

		List<String> lines = null;

		try {
			lines = Files.readAllLines(agentFile.toPath());
		} catch (IOException e) {
			LOGGER.fatal(e, e);
			System.exit(1);
		}

		LOGGER.info("pick random line from .agents file:");

		String randomLine = lines.get(rand.nextInt(lines.size()));

		String[] tmp = randomLine.split("-");

		PickedAagent = tmp[0].trim();
		PickedAagentType = tmp[1].trim();

		LOGGER.info("choosen agent - " + PickedAagent);
		LOGGER.info("choosen agent type - " + PickedAagentType);

		LOGGER.info("pick random agent end");
	}

	private void removeAllOtherAgents() {

		try {
			String cmd = MA_TO_PDDL_CONVERTOR + " " + domainAndProblemDirectoryPath + " " + domainName + " "
					+ problemName + " " + PickedAagent + " " + PickedAagentType + " " + MA_TO_PDDL_OUTPUT;

			LOGGER.info("RUN: " + cmd);

			Process pr = Runtime.getRuntime().exec(cmd);

			pr.waitFor();
		} catch (Exception e) {
			LOGGER.fatal(e, e);
			System.exit(1);
		}
	}

}
