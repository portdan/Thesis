package OurPlanner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.log4j.Logger;

public class TestDataAccumulator {

	private static final Logger LOGGER = Logger.getLogger(TestDataAccumulator.class);

	private static TestDataAccumulator currentAccumulator;

	private static String sep = ",";

	private final String domain;
	private final String problem;
	public List<String> agentNames;

	public Map<String,Long> agentLearningTimeMs = new HashMap<String, Long>();
	public Map<String,Long> agentPlanningTimeMs = new HashMap<String, Long>();

	public long totalTimeMs = 0;
	public int trainingSize = 0;
	public int numOfAgentsSolved = 0;
	public int numOfAgentsTimeout = 0;
	public int numOfAgentsNotSolved = 0;
	public int planLength = 0;
	public String solvingAgent = "";

	private String outFileName = null;


	private TestDataAccumulator(String domain, String problem, List<String> agentNames){
		this.domain = domain;
		this.problem = problem;
		this.agentNames = new ArrayList<String>(agentNames);
	}

	public static void startNewAccumulator(String domain, String problem, List<String> agentNames){
		currentAccumulator = new TestDataAccumulator(domain,problem, agentNames);
	}

	public static TestDataAccumulator getAccumulator(){
		return currentAccumulator;
	}

	public void setOutputFile(String output){
		outFileName = output;
	}

	public String getLabels(){
		StringBuilder sb = new StringBuilder();

		sb.append("Domain").append(sep);
		sb.append("Problem").append(sep);
		sb.append("Agents").append(sep);
		sb.append("Training Size").append(sep);
		sb.append("Solved").append(sep);
		sb.append("Timeout").append(sep);
		sb.append("Not Solved").append(sep);
		sb.append("Total Time").append(sep);

		for (String agentName : agentNames) {
			sb.append(agentName + " Learning Time").append(sep);
			sb.append(agentName + " Planning Time").append(sep);
		}

		sb.append("Solving Agent").append(sep);
		sb.append("Plan Length").append(sep);

		return sb.toString();
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append(domain).append(sep);
		sb.append(problem).append(sep);
		sb.append(agentNames.size()).append(sep);
		sb.append(trainingSize).append(sep);
		sb.append(numOfAgentsSolved).append(sep);
		sb.append(numOfAgentsTimeout).append(sep);
		sb.append(numOfAgentsNotSolved).append(sep);
		sb.append(totalTimeMs).append(sep);

		for (String agentName : agentNames) {
			sb.append(agentLearningTimeMs.get(agentName)).append(sep);
			sb.append(agentPlanningTimeMs.get(agentName)).append(sep);	
		}

		sb.append(solvingAgent).append(sep);
		sb.append(planLength).append(sep);

		return sb.toString();
	}

	public void writeOutput(){
		if(outFileName==null){
			LOGGER.info("WARN:not writing output!");
			return;
		}
		//write data accumulator output
		LOGGER.info("writing output...");
		File outFile = new File(outFileName);
		boolean newFile = false;
		if (!outFile.exists()) {
			try {
				outFile.createNewFile();
				newFile = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		PrintWriter writer = null;

		try {

			//OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8);
			//writer = new PrintWriter(outputStreamWriter,true);

			writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile,true)));
			if(newFile){
				writer.println(getLabels());
			}
			writer.write(TestDataAccumulator.getAccumulator().toString() + "\n");
			writer.flush();
			writer.close();
		} catch (IOException ex){
			// report
		} finally {
			try {writer.close();} catch (Exception ex) {}
		}
	}
}