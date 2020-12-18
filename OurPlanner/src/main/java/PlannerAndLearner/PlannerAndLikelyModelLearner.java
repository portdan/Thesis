package PlannerAndLearner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import OurPlanner.DeleteEffectGenerator;
import OurPlanner.ExecCommand;
import OurPlanner.Globals;
import OurPlanner.MADLAPlanner;
import OurPlanner.PlanToStateActionState;
import OurPlanner.PlanToStateActionStateResult;
import OurPlanner.PlanVerifier;
import OurPlanner.StateActionState;
import Utils.FileDeleter;
import Utils.TestDataAccumulator;
import Utils.VerificationResult;

public class PlannerAndLikelyModelLearner {

	private final static Logger LOGGER = Logger.getLogger(PlannerAndLikelyModelLearner.class);
	private final static Logger DEBUG_LOGGER = Logger.getLogger("DebugFile");

	private static final String LIKELY_MODEL_GENERATOR_SCRIPT = "__main__.py";
	private static final String PREPROCESS_PARAM = "-p";
	private static final String UPDATE_PARAM = "-u";
	private static final String CREATE_MODEL_PARAM = "-c";

	private static final String likelyModelGeneratorPath = Globals.LIKELY_MODEL_GENERATOR_PATH;
	private static final String inputFolderPath = likelyModelGeneratorPath
			+ Globals.LIKELY_MODEL_GENERATOR_INPUT_FOLDER;
	private static final String outputFolderPath = likelyModelGeneratorPath
			+ Globals.LIKELY_MODEL_GENERATOR_OUTPUT_FOLDER;
	private static final String workingFolderPath = likelyModelGeneratorPath
			+ Globals.LIKELY_MODEL_GENERATOR_WORKING_FOLDER;

	//private DeleteEffectGenerator DEGenerator = null;

	private File localViewFile = null;
	private File groundedFile = null;

	private String domainFileName = "";
	private String problemFileName = "";
	private String agentName = "";
	private String configFilePath = "";
	private List<String> agentList = null;

	public int num_agents_solved = 0;
	public int num_agents_not_solved = 0;
	public int num_agents_timeout = 0;

	private long startTimeMs = 0;
	private double timeoutInMS = 0;

	private int planningTimeoutInMS = 0;
	public int num_of_iterations = 0;
	public int num_of_added_traces = 0;

	public boolean isTimeout = false;

	private long sequancing_time_total = 0;
	private long sequancing_amount_total = 0;

	public long agentLearningTime = 0;

	public PlannerAndLikelyModelLearner(File groundedFile, File localViewFile, String domainFileName, String problemFileName,
			String configFilePath, String agentName, List<String> agentList,
			long startTimeMs, double timeoutInMS, int planningTimeoutInMS) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("PlannerAndLikelyModelLearner constructor");

		this.localViewFile = localViewFile;
		this.groundedFile = groundedFile;
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.configFilePath = configFilePath;
		this.agentName = agentName;
		this.agentList = agentList;
		this.startTimeMs = startTimeMs;
		this.timeoutInMS = timeoutInMS;
		this.planningTimeoutInMS = planningTimeoutInMS;

		logInput();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("localViewFile: " + localViewFile);
		LOGGER.info("groundedFile: " + groundedFile);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("agentName: " + agentName);
		LOGGER.info("startTimeMs: " + startTimeMs);
		LOGGER.info("timeoutInMS: " + timeoutInMS);
		LOGGER.info("planningTimeoutInMS: " + planningTimeoutInMS);
		LOGGER.info("likelyModelGeneratorPath: " + likelyModelGeneratorPath);
	}

	public static boolean preprocessData(String configFilePath) {
		LOGGER.info("preprocess Data");

		return runScript(configFilePath, PREPROCESS_PARAM);
	}

	public static boolean createModel(String configFilePath) {
		LOGGER.info("create Model");

		return runScript(configFilePath, CREATE_MODEL_PARAM);
	}

	public static boolean updateData(String configFilePath) {
		LOGGER.info("update Data");

		return runScript(configFilePath, UPDATE_PARAM);
	}

	private static boolean runScript(String configFilePath, String params) {
		try {
			String cmd = likelyModelGeneratorPath + "/" + LIKELY_MODEL_GENERATOR_SCRIPT + " " + params + " "
					+ configFilePath;
			LOGGER.info("RUN: " + cmd);
			// Process pr = Runtime.getRuntime().exec(cmd);
			// pr.waitFor();

			new ExecCommand(cmd);
		} catch (Exception e) {
			LOGGER.info(e, e);
			return false;

		}

		return true;
	}

	public List<String> plan() {
		
		DEBUG_LOGGER.debug("planning for agent: " + agentName);

		LOGGER.info("test");

		List<String> plan = null;
		long passedTimeMS = 0;

		Set<Model> used = new LinkedHashSet<Model>();

		copyProblemFiles();

		while (!isTimeout) {

			passedTimeMS = System.currentTimeMillis() - startTimeMs;

			/*
			 * if(!Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT) passedTimeMS =
			 * TestDataAccumulator.getAccumulator().
			 * getTotalTimeMSforAgentWithoutOfflineLearning(agentName) ; else passedTimeMS =
			 * TestDataAccumulator.getAccumulator().getTotalTimeMSforAgent(agentName) ;
			 */

			if (passedTimeMS > timeoutInMS) {

				LOGGER.fatal("TIMEOUT!");
				isTimeout = true;
				num_agents_solved = 0;
				num_agents_timeout = 1;
				num_agents_not_solved = 0;
				return null;
			}

			num_of_iterations += 1;

			String likelyModelDomainPath = Globals.OUTPUT_SOUND_MODEL_PATH + "/" + domainFileName;

			createModel(configFilePath);

			writeNewLikelyDomain(agentName, likelyModelDomainPath);

			Model likelyModel = new Model();

			if (!likelyModel.readModel(likelyModelDomainPath)) {
				LOGGER.fatal("provided path to liekly model domain file not existing");
				return null;
			}

			//			for (Model model : used) {
			//				if(model.equals(likelyModel))
			//					continue;
			//			}

			if(used.contains(likelyModel)) {
				DEBUG_LOGGER.debug("Model was seen before!");
				continue;
			}

			used.add(likelyModel);

			plan = planForAgent();
			
			LOGGER.info("Garbage collection!");
			System.gc();

			if (plan == null) {
				DEBUG_LOGGER.debug("Plan not generated!");				
				/*
				 * Set<TempModel> newModels = ExtendUnsafe(currModel,currTempModel);
				 * open.addAll(newModels); open.removeAll(closed);
				 */
			} else {

				VerificationResult res = verify(plan);												

				LOGGER.info("Garbage collection!");
				System.gc();

				if (res != null) {

					if (res.isTimeout) {
						isTimeout = true;
						num_agents_solved = 0;
						num_agents_timeout = 1;
						num_agents_not_solved = 0;
						return null;
					}

					if (res.isVerified) {
						LOGGER.info("GREAT!!");										

						num_agents_solved = 1;
						num_agents_timeout = 0;
						num_agents_not_solved = 0;
						return plan;
					} else {
						
						DEBUG_LOGGER.debug("Generated plan:");

						for (int i = 0; i <= res.lastOKActionIndex; i++) {
							DEBUG_LOGGER.debug("\t"+plan.get(i));
						}	

						DEBUG_LOGGER.debug("\t"+plan.get(res.lastOKActionIndex+1) + "<- Failed!");

						PlanToStateActionStateResult result = planToSASList(plan, res.lastOKActionIndex);

						if (result.isTimeout())
							continue;

						List<StateActionState> planSASList = result.getPlanSASList();
						StateActionState failedActionSAS = result.getFailedActionSAS();

						writeTracesToFiles(planSASList, failedActionSAS);

						long learningStartTime = System.currentTimeMillis();

						updateData(configFilePath);

						long totalLeariningTime = System.currentTimeMillis() - learningStartTime;

						agentLearningTime += totalLeariningTime;
					}
				} else {
					num_agents_solved = 0;
					num_agents_timeout = 0;
					num_agents_not_solved = 1;
					return null;
				}
			}
		}

		num_agents_solved = 0;
		num_agents_timeout = 0;
		num_agents_not_solved = 1;
		return null;

	}

	private void writeTracesToFiles(List<StateActionState> planSASList, StateActionState failedActionSAS) {

		String planTracesPath = workingFolderPath + "/PlanTraces.txt";
		writeSasListToFile(planSASList, planTracesPath);

		String failedTracePath = workingFolderPath + "/FailedTrace.txt";
		List<StateActionState> failedSASList = new ArrayList<>();
		failedSASList.add(failedActionSAS);
		writeSasListToFile(failedSASList, failedTracePath);
	}

	private PlanToStateActionStateResult planToSASList(List<String> plan, int lastOKActionIndex) {

		LOGGER.info("generate SAS List from plan");

		long sequancingStartTime = System.currentTimeMillis();

		long planToSASListTimeoutMS = (long) timeoutInMS
				- TestDataAccumulator.getAccumulator().agentPlanningTimeMs.get(agentName)
				- TestDataAccumulator.getAccumulator().agentLearningTimeMs.get(agentName)
				- TestDataAccumulator.getAccumulator().agentVerifingTimeMs.get(agentName);

		// planToSASListTimeoutMS = (long)timeoutInMS -
		// TestDataAccumulator.getAccumulator().getTotalTimeMSforAgent(agentName);

		String problemFilesPath = Globals.INPUT_GROUNDED_PATH;

		PlanToStateActionState plan2SAS = new PlanToStateActionState(domainFileName, problemFileName, problemFilesPath,
				sequancingStartTime, planToSASListTimeoutMS);

		List<StateActionState> planSASList = plan2SAS.generateSASList(plan, lastOKActionIndex);
		StateActionState failedActionSAS = null;

		if (planSASList == null)
			return new PlanToStateActionStateResult(null, null, true);

		if (lastOKActionIndex == planSASList.size() - 1)
			failedActionSAS = null;
		else if (lastOKActionIndex < planSASList.size()) {
			failedActionSAS = planSASList.get(lastOKActionIndex + 1);
			planSASList.remove(lastOKActionIndex + 1);
		}

		sequancing_amount_total = planSASList.size();

		long sequancingEndTime = System.currentTimeMillis();

		sequancing_time_total = sequancingEndTime - sequancingStartTime;

		if (!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;
		}

		return new PlanToStateActionStateResult(planSASList, failedActionSAS, false);
	}

	private VerificationResult verify(List<String> plan) {

		LOGGER.info("Verifing plan");

		String problemFilesPath = Globals.INPUT_GROUNDED_PATH;

		boolean useGrounded = true;

		long verifingStartTime = System.currentTimeMillis();

		long verifingTimeoutMS = (long) timeoutInMS
				- TestDataAccumulator.getAccumulator().agentPlanningTimeMs.get(agentName)
				- TestDataAccumulator.getAccumulator().agentLearningTimeMs.get(agentName);

		// verifingTimeoutMS = 5000;

		PlanVerifier planVerifier = new PlanVerifier(agentList, domainFileName, problemFileName, verifingTimeoutMS,
				problemFilesPath, useGrounded);

		VerificationResult res = planVerifier.verifyPlan(plan, 0);

		long verifingTotalTime = System.currentTimeMillis() - verifingStartTime;

		TestDataAccumulator.getAccumulator().totalVerifingTimeMs += verifingTotalTime;

		Long agentVerifingTime = TestDataAccumulator.getAccumulator().agentVerifingTimeMs.get(agentName);

		if (agentVerifingTime == null)
			agentVerifingTime = verifingTotalTime;
		else
			agentVerifingTime += verifingTotalTime;

		TestDataAccumulator.getAccumulator().agentVerifingTimeMs.put(agentName, agentVerifingTime);

		if (!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;
		}

		return res;
	}

	private List<String> planForAgent() {

		LOGGER.info("Planning for agent");

		String agentDomainPath = Globals.OUTPUT_SOUND_MODEL_PATH + "/" + domainFileName;
		String agentProblemPath = Globals.OUTPUT_SOUND_MODEL_PATH + "/" + problemFileName;

		String agentADDLPath = Globals.OUTPUT_TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";

		String heuristic = "saFF-glcl";
		int recursionLevel = -1;
		double timeLimitMin = ((double) planningTimeoutInMS) / 60000;

		long planningStartTime = System.currentTimeMillis();

		MADLAPlanner planner = new MADLAPlanner(agentDomainPath, agentProblemPath, agentADDLPath, heuristic,
				recursionLevel, timeLimitMin, agentList, agentName);

		List<String> result = planner.plan();

		/*
		 * if(planner.isTimeout) num_agents_timeout++;
		 * 
		 * if(planner.isNotSolved) num_agents_not_solved++;
		 */

		long planningTimeTotal = System.currentTimeMillis() - planningStartTime;

		TestDataAccumulator.getAccumulator().totalPlaningTimeMs += planningTimeTotal;

		Long agentPlanningTimes = TestDataAccumulator.getAccumulator().agentPlanningTimeMs.get(agentName);

		if (agentPlanningTimes == null)
			agentPlanningTimes = planningTimeTotal;
		else
			agentPlanningTimes += planningTimeTotal;

		TestDataAccumulator.getAccumulator().agentPlanningTimeMs.put(agentName, agentPlanningTimes);

		if (!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;
		}

		return result;
	}

	/***** HELPER FUNCTIONS *****/
	private boolean writeNewLikelyDomain(String agentName, String likelyDomainPath) {

		LOGGER.info("writing new likely .pddl domain file");

		String domainPath = localViewFile + "/" + agentName + "/" + domainFileName;

		String domainStr = readFileToString(domainPath);

		if (domainStr.isEmpty()) {
			LOGGER.info("Reading domain pddl failure");
			return false;
		}

		String likelyActionsString = generateActionsString(agentName);

		String likelyDomainStr = buildDomainString(domainStr, likelyActionsString);

		if (!writeStringToFile(likelyDomainPath, likelyDomainStr)) {
			LOGGER.info("Writing new likely domain to file failure");
			return false;
		}

		return true;
	}

	private Map<String, Set<String>> readActionsToPreconditions() {

		Map<String, Set<String>> res = new HashMap<String, Set<String>>();

		String likelyModelPath = outputFolderPath + "/LikelyModel.txt";

		List<String> actionsPreconditions = readFileToList(likelyModelPath);

		for (String actionPreconditions : actionsPreconditions) {
			String[] splited = actionPreconditions.split(":|;");

			String actionName = splited[0];
			Set<String> preconditions = new HashSet<String>();

			for (int i = 1; i < splited.length; i++) {
				preconditions.add(splited[i]);
			}

			res.put(actionName, preconditions);
		}

		return res;
	}

	private String getActionOwnerFromAction(String actionStr) {

		LOGGER.info("Extracting aftion owner from action " + actionStr);

		if(actionStr.contains(Globals.PARAMETER_INDICATION))
			actionStr = actionStr.replace(Globals.PARAMETER_INDICATION, " ");

		String[] split = actionStr.split(" ");

		String label = split[0];

		split = label.split(Globals.AGENT_INDICATION);

		return split[split.length-1];		
	}

	private boolean writeSasListToFile(List<StateActionState> SASList, String path) {

		LOGGER.info("Writing list to file");

		StringBuilder stringBuilder = new StringBuilder();

		for (StateActionState sas : SASList)
			stringBuilder.append(sas).append("\n");

		try {
			FileUtils.writeStringToFile(new File(path), stringBuilder.toString(), Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e, e);
			return false;
		}

		return true;
	}

	private String generateActionsString(String agentName) {

		LOGGER.info("Generating Actions For Models");

		Map<String, Set<String>> actionsToPreconditions = readActionsToPreconditions();

		Iterator<Entry<String, Set<String>>> it = actionsToPreconditions.entrySet().iterator();
		StringBuilder sb = new StringBuilder();

		DeleteEffectGenerator DEGenerator = new DeleteEffectGenerator (groundedFile, domainFileName, problemFileName);

		DEBUG_LOGGER.debug("Model for iteration: " + num_of_iterations);

		while (it.hasNext()) {
			Entry<String, Set<String>> actionPreconditions = (Entry<String, Set<String>>) it.next();

			String actionLabel = actionPreconditions.getKey();

			String actionOwner = getActionOwnerFromAction(actionLabel);

			if(!actionOwner.equals(agentName)) {
				Set<String> Pre = actionPreconditions.getValue();
				Set<String> Eff = DEGenerator.generateActionEffectsWithDeleteEffects(actionLabel);

				Pre = formatFacts(Pre);
				Eff = formatFacts(Eff);
				
				DEBUG_LOGGER.debug("\t" + actionLabel + " : ");
				DEBUG_LOGGER.debug("\t\t" + Pre);

				sb.append(generateAction(Pre, Eff, actionLabel, agentName));
			}
		}
		return sb.toString();
	}

	private Set<String> formatFacts(Set<String> facts) {

		LOGGER.info("Formatting facts");

		Set<String> formatted = new HashSet<String>();

		for (String fact : facts) {

			int startIndex = 0;
			int endIndex = fact.length();

			boolean isNegated = false;
			String formattedFact = fact;

			if (formattedFact.startsWith("not")) {
				isNegated = !isNegated;
				startIndex = formattedFact.indexOf('(');
				endIndex = formattedFact.lastIndexOf(')');
				formattedFact = formattedFact.substring(startIndex + 1, endIndex);
			}

			if (formattedFact.startsWith(Globals.NEGATED_KEYWORD)) {
				isNegated = !isNegated;
				formattedFact = formattedFact.replace(Globals.NEGATED_KEYWORD, "");
			}

			formattedFact = formattedFact.replace("(", " ");
			formattedFact = formattedFact.replace(",", "");
			formattedFact = formattedFact.replace(")", "");

			formattedFact = formattedFact.trim();

			formattedFact = '(' + formattedFact + ')';

			if (formattedFact.startsWith(Globals.NONE_KEYWORD)) {
				// formatted.addAll(formatNONEFact(formattedFact, isNegated));
				// formatted.add(formattedFact);
			} else {
				if (isNegated)
					formattedFact = "(not " + formattedFact + ")";

				formatted.add(formattedFact);
			}
		}

		return formatted;
	}

	private String generateAction(Set<String> pre, Set<String> eff, String action, String actionOwner) {

		LOGGER.info("Building action string");

		String rep = "";

		String actionName = action.replace(" ", Globals.PARAMETER_INDICATION);
		// String actionName = action;

		rep += "(:action " + actionName + "\n";
		rep += "\t:agent ?" + actionOwner + " - " + actionOwner + "\n";
		rep += "\t:parameters ()\n";

		if (pre.size() > 1)
			rep += "\t:precondition (and\n";
		else
			rep += "\t:precondition \n";
		if (pre.isEmpty())
			rep += "\t\t()\n";
		else
			for (String p : pre)
				rep += "\t\t" + p + "\n";
		if (pre.size() > 1)
			rep += "\t)\n";

		if (eff.size() > 1)
			rep += "\t:effect (and\n";
		else
			rep += "\t:effect \n";
		if (eff.isEmpty())
			rep += "\t\t()\n";
		else
			for (String e : eff)
				rep += "\t\t" + e + "\n";
		if (eff.size() > 1)
			rep += "\t)\n";

		rep += ")\n";
		return rep;
	}

	public static boolean setupLikelyModelGenerator(File tracesFile, List<String> agentList, Set<String> actionNames, Map<String, Set<String>> factGroups) {
		LOGGER.info("Setup LikelyModelGenerator files");

		if (!copyTracesFile(tracesFile))
			return false;

		if (!writeAgents(agentList))
			return false;

		if (!writeActions(actionNames))
			return false;

		if (!writeFactsGroups(factGroups))
			return false;

		return true;
	}

	private boolean copyProblemFiles() {

		LOGGER.info("Copy the output files");

		String agentDomainPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
		String agentProblemPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + problemFileName;

		File dstDir = new File(Globals.OUTPUT_SOUND_MODEL_PATH);

		File srcDir = new File(agentDomainPath);

		try {
			FileUtils.copyFileToDirectory(srcDir, dstDir);
		} catch (IOException e) {
			LOGGER.fatal(e, e);
			return false;
		}

		srcDir = new File(agentProblemPath);

		try {
			FileUtils.copyFileToDirectory(srcDir, dstDir);
		} catch (IOException e) {
			LOGGER.fatal(e, e);
			return false;
		}

		return true;
	}

	public static boolean clearLikelyModelGeneratorFiles() {
		return FileDeleter.cleanDirectory(outputFolderPath,false) && FileDeleter.cleanDirectory(workingFolderPath,false);
	}

	public static LikelyModelStatus readLikelyModelStatus(List<String> agentList) {

		String likelyModelStatusPath = outputFolderPath + "/Status.txt";

		List<String> content = null;

		Map<String, Boolean> agentActionsPresesnt = new HashMap<String, Boolean>();

		LikelyModelStatus status = new LikelyModelStatus(agentActionsPresesnt);

		try {
			content = Files.readAllLines(Paths.get(likelyModelStatusPath));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		for (int i = 0; i < content.size(); i++) {

			String line  = content.get(i);

			if(line.equals("agents all actions present:")) {

				for (int j = 0; j < agentList.size(); j++) {
					line  = content.get(i+j+1);
					String[] split = line.split(":");

					String agent = split[0].trim();
					String value = split[1].trim();

					if(value.equals("yes"))
						agentActionsPresesnt.put(agent, true);
					else if(value.equals("no"))
						agentActionsPresesnt.put(agent, false);
				}
				
				i +=agentList.size();
			}
		}

		return status;
	}

	private static boolean writeActions(Collection<String> actionNames) {

		LOGGER.info("Writing actions to data folder");

		String actionsFilePath = inputFolderPath + "/ActionNames.txt";

		return writeListToFile(actionsFilePath, actionNames);
	}

	private static boolean writeAgents(Collection<String> agentNames) {

		LOGGER.info("Writing agents to data folder");

		String actionsFilePath = inputFolderPath + "/Agents.txt";

		return writeListToFile(actionsFilePath, agentNames);
	}

	private static boolean writeFactsGroups(Map<String, Set<String>> factsGroups) {

		LOGGER.info("Writing facts groups to data folder");

		String factsGroupsFilePath = inputFolderPath + "/FactsGroups.txt";
		StringBuilder sb = new StringBuilder();

		Map<String, Set<String>> variableToValuesTreeMap = new TreeMap<String, Set<String>>(factsGroups);

		Iterator<Entry<String, Set<String>>> it = variableToValuesTreeMap.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, Set<String>> variableToValues = (Entry<String, Set<String>>) it.next();

			String var = variableToValues.getKey();
			Set<String> values = variableToValues.getValue();

			Set<String> valuesTreeSet = new TreeSet<>(values);

			sb.append(var + ":");

			for (String value : valuesTreeSet)
				sb.append(value).append(";");

			sb.append("\n");
		}

		return writeStringToFile(factsGroupsFilePath, sb.toString());
	}


	private static boolean copyTracesFile(File tracesFile) {

		LOGGER.info("Copy traces file to data folder");

		File[] directoryListing = tracesFile.listFiles();

		if (directoryListing != null) {
			for (File trace : directoryListing) {
				try {
					FileUtils.copyFileToDirectory(trace, new File(inputFolderPath));
				} catch (IOException e) {
					LOGGER.info(e, e);
					return false;
				}
			}
		} else
			return false;

		return true;
	}

	private String buildDomainString(String domainString, String actionsString) {

		LOGGER.info("Adding new actions to domain string");

		StringBuilder sb = new StringBuilder(domainString);

		int end = sb.lastIndexOf(")");

		if (end == -1)
			return "";
		else {
			sb.insert(end, actionsString);
			end += actionsString.length();
		}

		return sb.toString();
	}

	private static boolean writeListToFile(String filePath, Collection<String> list) {

		LOGGER.info("Writing list to file");

		StringBuilder stringBuilder = new StringBuilder();

		for (String str : list)
			stringBuilder.append(str).append("\n");

		try {
			FileUtils.writeStringToFile(new File(filePath), stringBuilder.toString(), Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e, e);
			return false;
		}

		return true;
	}

	private static boolean writeStringToFile(String filePath, String str) {

		LOGGER.info("Writing string to file");

		try {
			FileUtils.writeStringToFile(new File(filePath), str, Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e, e);
			return false;
		}

		return true;
	}

	private static String readFileToString(String filePath) {

		LOGGER.info("Reading file to string");

		String fileStr = "";

		try {
			fileStr = FileUtils.readFileToString(new File(filePath), Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e, e);
			return "";
		}

		return fileStr;
	}

	private static List<String> readFileToList(String filePath) {

		LOGGER.info("Reading file to list");

		List<String> fileStr = null;

		try {
			fileStr = FileUtils.readLines(new File(filePath), Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e, e);
			return null;
		}

		return fileStr;
	}
	/***** HELPER FUNCTIONS *****/


}