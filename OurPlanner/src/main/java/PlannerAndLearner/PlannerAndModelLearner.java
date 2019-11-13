package PlannerAndLearner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import OurPlanner.Globals;
import OurPlanner.MADLAPlanner;
import OurPlanner.PlanToStateActionState;
import OurPlanner.PlanToStateActionState2;
import OurPlanner.PlanVerifier;
import OurPlanner.StateActionState;
import OurPlanner.TestDataAccumulator;
import OurPlanner.TraceLearner;
import Utils.FileDeleter;
import Utils.VerificationResult;
import enums.IterationMethod;

public class PlannerAndModelLearner {

	private final static Logger LOGGER = Logger.getLogger(PlannerAndModelLearner.class);

	private	Random rnd = new Random();

	private String domainFileName = "";
	private String problemFileName = "";

	private String agentName = "";
	private List<String> agentList = null;

	private TraceLearner learner = null;

	private long sequancingTimeTotal = 0;
	private long sequancingAmountTotal = 0;

	public int num_agents_solved = 0;
	public int num_agents_not_solved = 0;
	public int num_agents_timeout = 0;

	private long startTimeMs = 0;
	private long timeoutInMS = 0;

	private IterationMethod iterationMethod = IterationMethod.Random;

	//	private boolean useSafeModel = true;

	private int index = -1;

	public boolean isTimeout = false;

	Model safeModel = null;
	Model unsafeModel = null;

	public PlannerAndModelLearner(String agentName, List<String> agentList, String domainFileName,
			String problemFileName, TraceLearner learner, IterationMethod iterationMethod, long startTimeMs, int timeoutInMS) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("PlannerAndModelLearner constructor");

		this.agentName = agentName;
		this.agentList = agentList;
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.learner = learner;
		this.startTimeMs = startTimeMs;
		this.timeoutInMS = timeoutInMS;
		this.iterationMethod = iterationMethod;

		logInput();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentName: " + agentName);
		LOGGER.info("agentList: " + agentList);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("iterationMethod: " + iterationMethod);
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


	public List<String> planAndLearn () {

		LOGGER.info("test");

		List<String> plan = null;

		if(!copyProblemFiles()) {
			LOGGER.info("Coping domain file failure");
			return null;
		}		

		String safeModelPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
		String unsafeModelPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		safeModel = new Model();
		unsafeModel = new Model();

		if(!safeModel.readModel(safeModelPath)) {
			LOGGER.fatal("provided path to safe model domain file not existing");
			return null;
		}

		if(!unsafeModel.readModel(unsafeModelPath)) {
			LOGGER.fatal("provided path to unsafe model domain file not existing");
			return null;
		}

		TempModel tmpModel  = new TempModel();

		Set<TempModel> open = new LinkedHashSet<TempModel>();
		Set<TempModel> closed = new LinkedHashSet<TempModel>();

		open.add(tmpModel);

		if(iterationMethod == IterationMethod.BFS)
			index = 0;
		else if(iterationMethod == IterationMethod.DFS)
			index = open.size() - 1;

		while (!open.isEmpty()) {

			if(System.currentTimeMillis() - startTimeMs > timeoutInMS){
				LOGGER.fatal("TIMEOUT!");
				isTimeout = true;
				return null;
			}

			TestDataAccumulator.getAccumulator().numOfIterations+=1;

			if(iterationMethod == IterationMethod.Random)
				index = rnd.nextInt(open.size());
			else if(iterationMethod == IterationMethod.Heuristic)
				index = getBestHeuristicIndex(open.toArray());

			TempModel currTempModel = (TempModel) (open.toArray())[index];

			open.remove(currTempModel);
			closed.add(currTempModel);	

			/*
			Model currModel = null;

			if(useSafeModel) {
				currModel = safeModel.extendModel(currTempModel);
				useSafeModel = false;
			}
			else
				currModel = unsafeModel.extendModel(currTempModel);
			 */

			Model currModel = unsafeModel.extendModel(currTempModel);

			writeDomainFile(currModel.reconstructModelString());

			plan = planForAgent();

			if(plan == null) {
				/*Set<TempModel> newModels = ExtendUnsafe(currModel,currTempModel);
				open.addAll(newModels);
				open.removeAll(closed);*/
			}
			else {

				VerificationResult res = verify(plan);

				if(res!=null) {

					if(res.isVerified) {
						LOGGER.info("GREAT!!");

						if(res.isVerified)
							num_agents_solved++;

						return plan;
					}
					else {

						List<StateActionState> planSASList = planToSASList(plan, res.lastActionIndex);
						StateActionState failedActionSAS = planSASList.get(res.lastActionIndex);

						planSASList.remove(res.lastActionIndex);						

						//useSafeModel = UpdateModels(planSASList);
						UpdateModels(planSASList);
						FilterOpenListModels(open, closed, planSASList, failedActionSAS);

						//if(res.lastActionIndex > 0) {
						Set<TempModel> newModels = ExtendSafe(currTempModel, failedActionSAS);
						open.addAll(newModels);		
						open.removeAll(closed);
						//}
					}
				}
				else
					return null;
			}
		}

		return null;
	}

	private int getBestHeuristicIndex(Object[] open) {

		int bestIndex = 0;
		int bestHeuristic = 0;

		for (int i = 0; i < open.length; i++) {

			TempModel tm = (TempModel) open[i];
			int h = calculateTempModelScore(tm);

			if(h > bestHeuristic) {
				bestHeuristic = h;
				bestIndex = i;
			}
		}

		return bestIndex;

	}

	private boolean UpdateModels(List<StateActionState> planSASList) {

		LOGGER.info("Updating safe and unsafe models with new plan training set");

		String safeModelPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
		String unsafeModelPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		learner.expandSafeAndUnSafeModelsWithPlan(planSASList, Globals.OUTPUT_SAFE_MODEL_PATH, Globals.OUTPUT_UNSAFE_MODEL_PATH, 
				sequancingTimeTotal, sequancingAmountTotal);

		safeModel.readModel(safeModelPath);
		unsafeModel.readModel(unsafeModelPath);

		return learner.IsSafeModelUpdated || learner.IsUnSafeModelUpdated;
	}

	private boolean FilterOpenListModels(Set<TempModel> open, Set<TempModel> closed,
			List<StateActionState> planSASList, StateActionState failedActionSAS) {

		LOGGER.info("Updating open list models with new plan training set");

		Set<TempModel> toRemove = new LinkedHashSet<TempModel>();

		for (TempModel tempModel : open) {

			Model testedModel = unsafeModel.extendModel(tempModel);

			// remove open list models that does not allow OK actions
			for (StateActionState sas : planSASList) {				
				if(!sas.actionOwner.equals(agentName) 
						//&& learner.LearnedActionsNames.contains(sas.action)
						){

					Action modelAction = testedModel.actions.get(sas.action); 
					Set<String> modelActionPre = new LinkedHashSet<String>(modelAction.preconditions);

					/*
					Set<String> modelActionEff = new LinkedHashSet<String>(modelAction.effects);

					Set<String> verifiedActionPre = new LinkedHashSet<String>(sas.pre);
					verifiedActionPre = formatFacts(verifiedActionPre);

					Set<String> verifiedActionEff = new LinkedHashSet<String>(sas.post);
					verifiedActionEff = formatFacts(verifiedActionEff);

					verifiedActionEff.removeAll(verifiedActionPre);


					if(!verifiedActionPre.containsAll(modelActionPre)) {
						toRemove.add(tempModel);
					}

					/*
					 else if(modelActionEff.containsAll(planActionEff)) {
						toRemove.add(tempModel);
					}

					 */


					Action safeModelAction = safeModel.actions.get(sas.action); 					
					Set<String> safeModelActionPre = new LinkedHashSet<String>(safeModelAction.preconditions);

					if(!safeModelActionPre.containsAll(modelActionPre)) {
						toRemove.add(tempModel);
					}

				}
			}

			/*
			if(!failedActionSAS.actionOwner.equals(agentName) 
					//&& learner.LearnedActionsNames.contains(failedActionSAS.action)
					) {

				// remove open list models that allow failed actions
				Action modelAction = testedModel.actions.get(failedActionSAS.action);

				Set<String> modelActionPre = new LinkedHashSet<String>(modelAction.preconditions);

				Set<String> modelActionEff = new LinkedHashSet<String>(modelAction.effects);

				Set<String> failedActionPre = new LinkedHashSet<String>(failedActionSAS.pre);
				failedActionPre = formatFacts(failedActionPre);

				Set<String> failedActionEff = new LinkedHashSet<String>(failedActionSAS.post);
				failedActionEff = formatFacts(failedActionEff);

				failedActionEff.removeAll(failedActionPre);

				if(failedActionPre.containsAll(modelActionPre)) {
					toRemove.add(tempModel);
				}

				//else if(!modelActionEff.containsAll(failedActionEff)) { 
				//	toRemove.add(tempModel);
				//}
			}
			 */


		}

		open.removeAll(toRemove);
		closed.addAll(toRemove);

		return true;
	}


	private Set<TempModel> ExtendSafe(TempModel currTempModel, StateActionState failedActionSAS) {

		LOGGER.info("Extending the model towards the Safe Model");

		Set<TempModel> res = new LinkedHashSet<TempModel>();
		Set<String> preconditions = null;

		String failedActionName = failedActionSAS.action;

		LOGGER.info(agentName + " " + failedActionName);

		preconditions = safeModel.actions.get(failedActionName).preconditions;
		Set<String> safePreconditions = new LinkedHashSet<String>(preconditions);

		preconditions = failedActionSAS.pre;
		Set<String> statePreconditions = new LinkedHashSet<String>(failedActionSAS.pre);

		statePreconditions = formatFacts(statePreconditions);

		safePreconditions.removeAll(statePreconditions);
		preconditions = new LinkedHashSet<String>(safePreconditions);		 

		for (String pre : preconditions) {

			TempModel tempModel = new TempModel(currTempModel);

			//TempAction tempAction = tempModel.getTempActionByName(failedActionName);
			TempAction tempAction = tempModel.popTempActionByName(failedActionName);

			if(tempAction == null)
				tempAction = new TempAction();

			if(!tempAction.preconditionsAdd.contains(pre) && !tempAction.preconditionsSub.contains(pre)) {

				tempAction.name = failedActionName;

				tempAction.preconditionsAdd.add(pre);

				tempModel.tempActions.add(tempAction);

				res.add(tempModel);
			}
		}

		return res;
	}

	private Set<String> formatFacts(Set<String> facts) {

		LOGGER.info("Formatting facts");

		Set<String> formatted = new HashSet<String>();

		for (String fact : facts) {

			int startIndex = 0;
			int endIndex = fact.length();

			boolean isNegated = false;
			String formattedFact = fact;

			if(formattedFact.startsWith("not")) {
				isNegated = !isNegated;
				startIndex = formattedFact.indexOf('(');
				endIndex = formattedFact.lastIndexOf(')');			
				formattedFact = formattedFact.substring(startIndex+1,endIndex);
			}

			if(formattedFact.startsWith(Globals.NEGATED_KEYWORD)) {
				isNegated = !isNegated;
				formattedFact = formattedFact.replace(Globals.NEGATED_KEYWORD, "");
			}

			formattedFact = formattedFact.replace("(", " ");
			formattedFact = formattedFact.replace(",", "");
			formattedFact = formattedFact.replace(")", "");

			formattedFact = formattedFact.trim();

			formattedFact = '(' + formattedFact + ')';

			if(formattedFact.startsWith(Globals.NONE_KEYWORD)) {
				//formatted.addAll(formatNONEFact(formattedFact, isNegated));
				//formatted.add(formattedFact);
			}
			else {
				if (isNegated)
					formattedFact = "(not " + formattedFact + ")";

				formatted.add(formattedFact);
			}
		}

		return formatted;
	}

	private Set<TempModel> ExtendUnsafe(Model currModel, TempModel currTempModel) {

		LOGGER.info("Extending the model towards the Unsafe Model");

		Set<TempModel> res = new LinkedHashSet<TempModel>();
		Set<String> preconditions = null;

		for (Entry<String, Action> pair : currModel.actions.entrySet()) {

			Action action = pair.getValue();

			preconditions = currModel.actions.get(action.name).preconditions;
			Set<String> currPreconditions = new LinkedHashSet<String>(preconditions);			

			preconditions = unsafeModel.actions.get(action.name).preconditions;
			Set<String> unsafePreconditions = new LinkedHashSet<String>(preconditions);			

			currPreconditions.removeAll(unsafePreconditions);
			preconditions = new LinkedHashSet<String>(currPreconditions);			 

			for (String pre : preconditions) {

				TempModel tempModel = new TempModel(currTempModel);

				//TempAction tempAction = tempModel.getTempActionByName(action.name);
				TempAction tempAction = tempModel.popTempActionByName(action.name);

				if(tempAction == null)
					tempAction = new TempAction();

				if(!tempAction.preconditionsSub.contains(pre) && !tempAction.preconditionsAdd.contains(pre)) {

					tempAction.name = action.name;

					tempAction.preconditionsSub.add(pre);

					tempModel.tempActions.add(tempAction);

					res.add(tempModel);
				}

			}
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
		double timeLimitMin = 0.2;

		long planningStartTime = System.currentTimeMillis();

		MADLAPlanner planner = new MADLAPlanner(agentDomainPath, agentProblemPath, agentADDLPath,
				heuristic, recursionLevel, timeLimitMin, agentList, agentName);

		List<String> result = planner.plan();

		if(planner.isTimeout)
			num_agents_timeout++;

		if(planner.isNotSolved)
			num_agents_not_solved++;

		long planningFinishTime = System.currentTimeMillis();

		TestDataAccumulator.getAccumulator().totalPlaningTimeMs += planningFinishTime - planningStartTime;

		Long agentPlanningTimes = TestDataAccumulator.getAccumulator().agentPlanningTimeMs.get(agentName);

		if(agentPlanningTimes == null) 
			agentPlanningTimes = planningFinishTime - planningStartTime;
		else
			agentPlanningTimes += planningFinishTime - planningStartTime;

		TestDataAccumulator.getAccumulator().agentPlanningTimeMs.put(agentName, agentPlanningTimes);		

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;
		}

		return result;
	}

	private VerificationResult verify(List<String> plan) {

		LOGGER.info("Verifing plan");

		String problemFilesPath = Globals.INPUT_GROUNDED_PATH;

		boolean useGrounded = true;

		PlanVerifier planVerifier = new PlanVerifier(agentList,domainFileName,problemFileName,
				problemFilesPath, useGrounded);		

		VerificationResult res = planVerifier.verifyPlan(plan,0);

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;	
		}

		return res;
	}

	private List<StateActionState> planToSASList(List<String> plan, int lastActionIndex) {

		LOGGER.info("generate SAS List from plan");

		long sequancingStartTime = System.currentTimeMillis();

		String problemFilesPath = Globals.INPUT_GROUNDED_PATH;

		PlanToStateActionState plan2SAS = new PlanToStateActionState(domainFileName, problemFileName, problemFilesPath);

		problemFilesPath = Globals.OUTPUT_SOUND_MODEL_PATH;
		PlanToStateActionState2 plan2SAS2 = new PlanToStateActionState2(domainFileName, problemFileName,
				problemFilesPath, agentName);

		List<StateActionState> res = plan2SAS.generateSASList(plan, lastActionIndex);
		//List<StateActionState> res = plan2SAS2.generateSASList(plan, lastActionIndex);

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;	
		}

		long sequancingEndTime = System.currentTimeMillis();

		sequancingTimeTotal = sequancingEndTime - sequancingStartTime;
		sequancingAmountTotal = res.size();

		return res;
	}

	private boolean writeDomainFile(String newDomainString) {

		LOGGER.info("Writing new PDDL domain file");

		String agentDomainPath = Globals.OUTPUT_SOUND_MODEL_PATH + "/" + domainFileName;

		try {
			FileUtils.writeStringToFile(new File(agentDomainPath), newDomainString, Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return false;
		}

		return true;
	}

	private int calculateModelScore(Model model) {

		int score = 0;

		for (Entry<String, Action> pair : model.actions.entrySet()) {

			String actionName = pair.getKey();
			Action action = pair.getValue();

			Map<String, Integer> actionPreconditionScore = learner.getActionPreconditionsScore(actionName);

			if(actionPreconditionScore != null)
				for (String pre : action.preconditions)
					score += actionPreconditionScore.get(pre);

		}

		return score;
	}

	private int calculateTempModelScore(TempModel tempModel) {

		int score = 0;

		for (TempAction tempAct : tempModel.tempActions) {

			Map<String, Integer> actionPreconditionScore = learner.getActionPreconditionsScore(tempAct.name);

			if(actionPreconditionScore != null)
				for (String pre : tempAct.preconditionsAdd)
					score += actionPreconditionScore.get(pre);
		}

		return score;
	}
}
