package Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import OurPlanner.Globals;
import enums.IterationMethod;

public class TestDataAccumulator {

	private static final Logger LOGGER = Logger.getLogger(TestDataAccumulator.class);

	private static TestDataAccumulator currentAccumulator;

	private static String sep = ",";

	private final String domain;
	private final String problem;
	public List<String> agentNames;

	public Map<String, Long> agentLearningTimeMs = new HashMap<String, Long>();
	public Map<String, Long> agentOfflineLearningTimeMs = new HashMap<String, Long>();
	public Map<String, Long> agentPlanningTimeMs = new HashMap<String, Long>();
	public Map<String, Long> agentVerifingTimeMs = new HashMap<String, Long>();
	public Map<String, Integer> agentAddedTrainingSize = new HashMap<String, Integer>();
	public Map<String, Integer> agentNumOfIterations = new HashMap<String, Integer>();
	public Map<String, Boolean> agentAllActionsPresent = new HashMap<String, Boolean>();

	public long totalTimeMs = 0;
	public long totalLearningTimeMs = 0;
	public long totalPlaningTimeMs = 0;
	public long totalVerifingTimeMs = 0;
	public int initialTrainingSize = 0;
	public double trainingSizeBucket = 0;
	public int numOfAgentsSolved = 0;
	public int numOfAgentsTimeout = 0;
	public int numOfAgentsNotSolved = 0;
	public int planLength = 0;
	public String solvingAgent = "";
	public String finishStatus = "";
	public String method = "";
	public String experimentDetails = "";

	private String outFileName = null;

	private TestDataAccumulator(String domain, String problem, List<String> agentNames) {

		LOGGER.setLevel(Level.INFO);

		this.domain = domain;
		this.problem = problem;
		this.agentNames = new ArrayList<String>(agentNames);

		agentLearningTimeMs = new HashMap<String, Long>();
		agentOfflineLearningTimeMs = new HashMap<String, Long>();
		agentPlanningTimeMs = new HashMap<String, Long>();
		agentVerifingTimeMs = new HashMap<String, Long>();
		agentAddedTrainingSize = new HashMap<String, Integer>();
		agentNumOfIterations = new HashMap<String, Integer>();

		for (String agent : agentNames) {
			agentLearningTimeMs.put(agent, 0l);
			agentOfflineLearningTimeMs.put(agent, 0l);
			agentPlanningTimeMs.put(agent, 0l);
			agentVerifingTimeMs.put(agent, 0l);
			agentAddedTrainingSize.put(agent, 0);
			agentNumOfIterations.put(agent, 0);
		}
	}

	public static void startNewAccumulator(String domain, String problem, List<String> agentNames) {
		currentAccumulator = new TestDataAccumulator(domain, problem, agentNames);
	}

	public static TestDataAccumulator getAccumulator() {
		return currentAccumulator;
	}

	/*
	public long getTotalTimeMSforAgent(String agentName) {
		return agentLearningTimeMs.get(agentName) + agentPlanningTimeMs.get(agentName) + agentVerifingTimeMs.get(agentName);
	}

	public long getTotalTimeMSforAgentWithoutOfflineLearning(String agentName) {
		return agentLearningTimeMs.get(agentName) - agentOfflineLearningTimeMs.get(agentName) + agentPlanningTimeMs.get(agentName) + agentVerifingTimeMs.get(agentName);
	}

	public long getTotalTimeMS() {
		long res = 0;
		for (String agent : agentNames) 
			res += getTotalTimeMSforAgent(agent);

		return res;
	}

	public long getTotalTimeMSWithoutOfflienLearning() {
		long res = 0;
		for (String agent : agentNames) 
			res += getTotalTimeMSforAgentWithoutOfflineLearning(agent);

		return res;
	}
	 */

	public long getOfflineLearningTime() {
		long res = 0;
		for (String agent : agentNames) 
			res += agentOfflineLearningTimeMs.get(agent);

		return res;
	}

	public void setOutputFile(String output) {
		outFileName = output;
	}

	public String getLabels() {
		StringBuilder sb = new StringBuilder();

		sb.append("Domain").append(sep);
		sb.append("Problem").append(sep);
		sb.append("Agents").append(sep);
		sb.append("Details").append(sep);
		sb.append("Initial Training Size").append(sep);
		sb.append("Training Size Bucket").append(sep);
		sb.append("Iteration Method").append(sep);
		sb.append("Status").append(sep);
		sb.append("Solved").append(sep);
		sb.append("Timeout").append(sep);
		sb.append("Not Solved").append(sep);
		sb.append("Total Time").append(sep);

		if(Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT)
			sb.append("Total Time - Learning").append(sep);

		sb.append("Total Learning Time").append(sep);
		sb.append("Total Planning Time").append(sep);
		sb.append("Total Verifing Time").append(sep);

		for (String agentName : agentNames) {
			sb.append(agentName + " Learning Time").append(sep);
			sb.append(agentName + " Planning Time").append(sep);
			sb.append(agentName + " Verifing Time").append(sep);
			sb.append(agentName + " Added Training Size").append(sep);
			sb.append(agentName + " Number of Iterations").append(sep);
			sb.append(agentName + " All Actions Present").append(sep);
		}

		sb.append("Solving Agent").append(sep);
		sb.append("Solving Agent Number of Iterations").append(sep);
		sb.append("Solving Agent All Actions Present").append(sep);
		sb.append("Plan Length").append(sep);

		return sb.toString();
	}

	public String toString() {

		numOfAgentsSolved = 0;
		numOfAgentsTimeout = 0;
		numOfAgentsNotSolved = 0;

		if(finishStatus.equals("SOLVED"))
			numOfAgentsSolved = 1;
		if(finishStatus.equals("TIMEOUT"))
			numOfAgentsTimeout = 1;
		if(finishStatus.equals("NOT SOLVED"))
			numOfAgentsNotSolved = 1;

		StringBuilder sb = new StringBuilder();

		sb.append(domain).append(sep);
		sb.append(problem).append(sep);
		sb.append(agentNames.size()).append(sep);
		sb.append(experimentDetails).append(sep);
		sb.append(initialTrainingSize).append(sep);
		sb.append(trainingSizeBucket).append(sep);
		sb.append(method).append(sep);
		sb.append(finishStatus).append(sep);
		sb.append(numOfAgentsSolved).append(sep);
		sb.append(numOfAgentsTimeout).append(sep);
		sb.append(numOfAgentsNotSolved).append(sep);
		sb.append(totalTimeMs).append(sep);

		if(Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT)
			sb.append(totalTimeMs - getOfflineLearningTime()).append(sep);

		sb.append(totalLearningTimeMs).append(sep);
		sb.append(totalPlaningTimeMs).append(sep);
		sb.append(totalVerifingTimeMs).append(sep);

		for (String agentName : agentNames) {
			sb.append(agentLearningTimeMs.get(agentName)).append(sep);
			sb.append(agentPlanningTimeMs.get(agentName)).append(sep);
			sb.append(agentVerifingTimeMs.get(agentName)).append(sep);
			sb.append(agentAddedTrainingSize.get(agentName)).append(sep);
			sb.append(agentNumOfIterations.get(agentName)).append(sep);
			sb.append(agentAllActionsPresent.get(agentName)).append(sep);
		}

		sb.append(solvingAgent).append(sep);
		sb.append(agentNumOfIterations.get(solvingAgent)).append(sep);
		sb.append(agentAllActionsPresent.get(solvingAgent)).append(sep);
		sb.append(planLength).append(sep);

		return sb.toString();
	}

	public void writeOutput() {
		if (outFileName == null) {
			LOGGER.info("WARN:not writing output!");
			return;
		}
		// write data accumulator output
		LOGGER.info("writing output...");
		File outFile = new File(outFileName);
		boolean newFile = false;
		if (!outFile.exists()) {
			try {
				outFile.createNewFile();
				newFile = true;
			} catch (IOException e) {
				LOGGER.fatal(e, e);
				e.printStackTrace();
			}
		}

		PrintWriter writer = null;

		try {

			// OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new
			// FileOutputStream(outFile), StandardCharsets.UTF_8);
			// writer = new PrintWriter(outputStreamWriter,true);

			writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile, true)));
			if (newFile) {
				writer.println(getLabels());
			}
			writer.write(TestDataAccumulator.getAccumulator().toString() + "\n");
			writer.flush();
			writer.close();
		} catch (IOException ex) {
			// report
		} finally {
			try {
				writer.close();
			} catch (Exception ex) {
			}
		}
	}
}
