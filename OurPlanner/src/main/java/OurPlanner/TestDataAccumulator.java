package OurPlanner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class TestDataAccumulator {

	private static final Logger LOGGER = Logger.getLogger(TestDataAccumulator.class);

	private static TestDataAccumulator currentAccumulator = new TestDataAccumulator("", "");

	private static String sep = ",";

	private final String domain;
	private final String problem;

	public long startTimeMs = 0;
	public long finishTimeMs = 0;
	public int numOfAgents = 0;
	public int trainingSize = 0;
	public int numOfAgentsSolved = 0;
	public int numOfAgentsTimeout = 0;
	public int numOfAgentsNotSolved = 0;
	public int planLength = 0;
	public boolean planVerified = false;

	private String outFileName = null;


	private TestDataAccumulator(String domain, String problem){
		this.domain = domain;
		this.problem = problem;
	}



	public static void startNewAccumulator(String domain, String problem){
		currentAccumulator = new TestDataAccumulator(domain,problem);
	}

	public static TestDataAccumulator getAccumulator(){
		return currentAccumulator;
	}

	public void setOutputFile(String output){
		outFileName = output;
	}

	public static String getLabels(){
		StringBuilder sb = new StringBuilder();

		sb.append("Domain").append(sep);
		sb.append("Problem").append(sep);
		sb.append("Agents").append(sep);
		sb.append("Training Size").append(sep);
		sb.append("Solved").append(sep);
		sb.append("Timeout").append(sep);
		sb.append("NotSolved").append(sep);
		sb.append("TotalTime").append(sep);
		sb.append("Plan Verified").append(sep);
		sb.append("Plan Length").append(sep);

		return sb.toString();
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append(domain).append(sep);
		sb.append(problem).append(sep);
		sb.append(numOfAgents).append(sep);
		sb.append(trainingSize).append(sep);
		sb.append(numOfAgentsSolved).append(sep);
		sb.append(numOfAgentsTimeout).append(sep);
		sb.append(numOfAgentsNotSolved).append(sep);
		sb.append(finishTimeMs-startTimeMs).append(sep);
		sb.append(planVerified).append(sep);
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
		LOGGER.info(TestDataAccumulator.getAccumulator().toString());
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
			writer = new PrintWriter(new BufferedWriter(new FileWriter(outFile,true)));
			if(newFile){
				writer.println(TestDataAccumulator.getLabels());
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
