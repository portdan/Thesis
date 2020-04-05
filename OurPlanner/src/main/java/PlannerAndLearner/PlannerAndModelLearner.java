package PlannerAndLearner;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import OurPlanner.Globals;
import OurPlanner.MADLAPlanner;
import OurPlanner.PlanToStateActionState;
import OurPlanner.PlanToStateActionStateResult;
import OurPlanner.PlanVerifier;
import OurPlanner.StateActionState;
import OurPlanner.TraceLearner;
import Utils.ArrayUtils;
import Utils.FileDeleter;
import Utils.MCTS;
import Utils.OSStatistics;
import Utils.TestDataAccumulator;
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
	private Set<String> goalFacts;

	private long sequancingTimeTotal = 0;
	private long sequancingAmountTotal = 0;

	public int num_agents_solved = 0;
	public int num_agents_not_solved = 0;
	public int num_agents_timeout = 0;

	private long startTimeMs = 0;
	private double timeoutInMS = 0;
	private int planningTimeoutInMS;
	public int numOfIterations = 0;
	public int addedTrainingSize = 0;

	//	private boolean useSafeModel = true;

	private int index = -1;

	public boolean isTimeout = false;

	Model safeModel = null;
	Model unsafeModel = null;

	OperatingSystemMXBean OSstatistics = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

	public PlannerAndModelLearner(String agentName, List<String> agentList, String domainFileName,
			String problemFileName, TraceLearner learner, Set<String> goalFacts,
			long startTimeMs, double timeoutInMS, int planningTimeoutInMS) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("PlannerAndModelLearner constructor");

		this.agentName = agentName;
		this.agentList = agentList;
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.learner = learner;
		this.startTimeMs = startTimeMs;
		this.timeoutInMS = timeoutInMS;
		this.planningTimeoutInMS = planningTimeoutInMS;
		this.goalFacts = goalFacts;

		logInput();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentName: " + agentName);
		LOGGER.info("agentList: " + agentList);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("startTimeMs: " + startTimeMs);
		LOGGER.info("timeoutInMS: " + timeoutInMS);
		LOGGER.info("planningTimeoutInMS: " + planningTimeoutInMS);
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


	public List<String> planAndLearn (IterationMethod method) {

		LOGGER.info("test");

		List<String> plan = null;
		long passedTimeMS = 0;

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

		ModelSearchNode searchNode = new ModelSearchNode(null, new TempModel());

		Set<ModelSearchNode> open = new LinkedHashSet<ModelSearchNode>();
		Set<ModelSearchNode> closed = new LinkedHashSet<ModelSearchNode>();

		open.add(searchNode);

		while (!open.isEmpty()) {

			passedTimeMS = System.currentTimeMillis() - startTimeMs;

			/*
			if(!Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT)
				passedTimeMS = TestDataAccumulator.getAccumulator().getTotalTimeMSforAgentWithoutOfflineLearning(agentName) ;
			else
				passedTimeMS = TestDataAccumulator.getAccumulator().getTotalTimeMSforAgent(agentName) ;
			 */

			if(passedTimeMS > timeoutInMS){
				LOGGER.fatal("TIMEOUT!");
				isTimeout = true;
				num_agents_solved = 0;
				num_agents_timeout = 1;
				num_agents_not_solved = 0;
				return null;
			}

			numOfIterations += 1;

			switch (method) {
			case BFS:
				index = 0;
				break;
			case DFS:
				index = open.size() - 1;
				break;
			case Random:
				index = rnd.nextInt(open.size());
				break;
			case Goal_Proximity_Heuristic:
			case Reliability_Heuristic:
			case Plan_Length_Heuristic:
			case Plan_Length_And_Reliability_Heuristic:
			default:
				index = getBestHeuristicIndex(open.toArray(), method);

				break;
			}

			searchNode = (ModelSearchNode) (open.toArray())[index];

			open.remove(searchNode);
			closed.add(searchNode);	

			/*
			Model currModel = null;

			if(useSafeModel) {
				currModel = safeModel.extendModel(currTempModel);
				useSafeModel = false;
			}
			else
				currModel = unsafeModel.extendModel(currTempModel);
			 */

			Model currModel = unsafeModel.extendModel(searchNode.getTempModel());

			writeDomainFile(currModel.reconstructModelString());

			plan = planForAgent();

			LOGGER.info("Garbage collection!");
			System.gc();

			if(plan == null) {
				/*Set<TempModel> newModels = ExtendUnsafe(currModel,currTempModel);
				open.addAll(newModels);
				open.removeAll(closed);*/
			}
			else {

				VerificationResult res = verify(plan);

				LOGGER.info("Garbage collection!");
				System.gc();

				if(res!=null) {

					if(res.isTimeout) {
						isTimeout = true;
						num_agents_solved = 0;
						num_agents_timeout = 1;
						num_agents_not_solved = 0;
						return null;
					}

					if(res.isVerified) {
						LOGGER.info("GREAT!!");

						num_agents_solved = 1;
						num_agents_timeout=0;
						num_agents_not_solved=0;						
						return plan;
					}
					else {

						PlanToStateActionStateResult result = planToSASList(plan, res.lastActionIndex);

						if(result.isTimeout())
							continue;

						List<StateActionState> planSASList = result.getPlanSASList();
						StateActionState failedActionSAS = result.getFailedActionSAS();

						searchNode.setPlanSASList(planSASList);
						searchNode.calcGoalProximity(goalFacts);

						//useSafeModel = UpdateModels(planSASList);
						UpdateModels(planSASList);
						FilterOpenListModels(open, closed, planSASList, failedActionSAS);

						//if(res.lastActionIndex > 0) {
						Set<ModelSearchNode> newModels = ExtendSafe(searchNode, failedActionSAS);
						open.addAll(newModels);		
						open.removeAll(closed);
						//}
					}
				}
				else {
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

	public List<String> planAndLearnMonteCarlo(IterationMethod method, int Cvalue) {

		LOGGER.info("test");

		List<String> plan = null;
		long passedTimeMS = 0;

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

		ModelSearchNode searchNodeRoot = new ModelSearchNode(null, new TempModel());

		MCTS mcts = new MCTS(searchNodeRoot, Cvalue);

		while (!isTimeout) {

			passedTimeMS = System.currentTimeMillis() - startTimeMs;

			/*
			if(!Globals.IGNORE_OFFLINE_LEARNING_TIMEOUT)
				passedTimeMS = TestDataAccumulator.getAccumulator().getTotalTimeMSforAgentWithoutOfflineLearning(agentName) ;
			else
				passedTimeMS = TestDataAccumulator.getAccumulator().getTotalTimeMSforAgent(agentName) ;
			 */

			if(passedTimeMS > timeoutInMS){

				LOGGER.fatal("TIMEOUT!");
				isTimeout = true;
				num_agents_solved = 0;
				num_agents_timeout = 1;
				num_agents_not_solved = 0;
				return null;
			}

			/*
			double getCommittedVirtualMemorySize = (double)OSstatistics.getCommittedVirtualMemorySize()/1073741824;
			double getFreePhysicalMemorySize = (double)OSstatistics.getFreePhysicalMemorySize()/1073741824;
			double getTotalPhysicalMemorySize = (double)OSstatistics.getTotalPhysicalMemorySize()/1073741824;

			double usedMemoryRatio = (double)(getTotalPhysicalMemorySize-getFreePhysicalMemorySize)/(double)getTotalPhysicalMemorySize;
			 */

			double usedMemoryRatio = OSStatistics.GetMemoryUsage();

			if(usedMemoryRatio > Globals.MEMORY_OVER_USAGE_RATIO) {

				LOGGER.fatal("MEMORY OVER USAGE!");
				isTimeout = true;
				num_agents_solved = 0;
				num_agents_timeout = 1;
				num_agents_not_solved = 0;
				return null;
			}

			numOfIterations += 1;

			ModelSearchNode searchNode = mcts.selectBestNode(searchNodeRoot);

			Model currModel = unsafeModel.extendModel(searchNode.getTempModel());

			writeDomainFile(currModel.reconstructModelString());

			plan = planForAgent();

			if(plan == null) {
				//mcts.backpropogateNode(searchNode, searchNode.getReliabilityHeuristic(learner.actionsPreconditionsScore));					
				mcts.removeNode(searchNode);
			}
			else {

				VerificationResult res = verify(plan);

				if(res!=null) {

					if(res.isTimeout) {
						continue;
					}
					if(res.isVerified) {
						LOGGER.info("GREAT!!");
						num_agents_solved = 1;
						num_agents_timeout=0;
						num_agents_not_solved=0;
						return plan;
					}
					else {

						PlanToStateActionStateResult result = planToSASList(plan, res.lastActionIndex);

						if(result.isTimeout())
							continue;

						List<StateActionState> planSASList = result.getPlanSASList();
						StateActionState failedActionSAS = result.getFailedActionSAS();

						searchNode.setPlanSASList(planSASList);
						searchNode.calcGoalProximity(goalFacts);

						UpdateModels(planSASList);
						FilterMCTSModels(mcts, searchNodeRoot, planSASList);

						Set<ModelSearchNode> newModels = ExtendSafe(searchNode, failedActionSAS);

						if(newModels.isEmpty())
							mcts.removeNode(searchNode);
						else {
							searchNode.setChildren(new ArrayList<ModelSearchNode>(newModels));

							//mcts.backpropogateNode(searchNode, calcHueristicAverage(searchNode));
							mcts.backpropogateNode(searchNode, calcNodeHeuristic(searchNode, method), 1);					
						}
					}
				}
				else {
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

	private int getBestHeuristicIndex(Object[] open, IterationMethod method) {

		int bestIndex = 0;

		int[] rh = new int[open.length];
		int[] gph = new int[open.length];
		int[] plh = new int[open.length];

		for (int i = 0; i < open.length; i++) {
			ModelSearchNode sn = (ModelSearchNode) open[i];

			rh[i] = sn.getReliabilityHeuristic(learner.actionsPreconditionsScore);
			gph[i] = sn.getGoalProximityHeuristic();
			plh[i] = sn.getPlanLengthHeuristic();
		}

		if(method == IterationMethod.Goal_Proximity_Heuristic)
			bestIndex = ArrayUtils.findIndexOfMax(gph);

		if(method == IterationMethod.Reliability_Heuristic)
			bestIndex = ArrayUtils.findIndexOfMax(rh);

		if(method == IterationMethod.Plan_Length_Heuristic)
			bestIndex = ArrayUtils.findIndexOfMax(plh);

		if(method == IterationMethod.Plan_Length_And_Reliability_Heuristic) {

			List<Integer> indices = ArrayUtils.findIndicesOfMax(plh);

			int[] plarh = ArrayUtils.subsetByIndices(rh, indices);

			int bestPLaRH = ArrayUtils.findIndexOfMax(plarh);

			bestIndex = indices.get(bestPLaRH);
		}

		return bestIndex;
	}

	private int calcNodeHeuristic(ModelSearchNode searchNode, IterationMethod method) {


		if(method == IterationMethod.Goal_Proximity_Heuristic)
			return searchNode.getGoalProximityHeuristic();
		if(method == IterationMethod.Reliability_Heuristic)
			return searchNode.getReliabilityHeuristic(learner.actionsPreconditionsScore);
		if(method == IterationMethod.Plan_Length_Heuristic)
			return searchNode.getPlanLengthHeuristic();

		return 0;
	}

	private boolean UpdateModels(List<StateActionState> planSASList) {

		LOGGER.info("Updating safe and unsafe models with new plan training set");

		boolean res = false;

		String safeModelPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
		String unsafeModelPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		if(planSASList != null && planSASList.size() > 0) {
			learner.expandSafeAndUnSafeModelsWithPlan(planSASList, Globals.OUTPUT_SAFE_MODEL_PATH, Globals.OUTPUT_UNSAFE_MODEL_PATH, 
					sequancingTimeTotal, sequancingAmountTotal);

			addedTrainingSize += planSASList.size();

			safeModel.readModel(safeModelPath);
			unsafeModel.readModel(unsafeModelPath);

			res =  learner.IsSafeModelUpdated || learner.IsUnSafeModelUpdated;
		}

		LOGGER.info("Garbage collection!");
		System.gc();

		return res;
	}

	private boolean FilterOpenListModels(Set<ModelSearchNode> open, Set<ModelSearchNode> closed,
			List<StateActionState> planSASList, StateActionState failedActionSAS) {

		LOGGER.info("Updating open list models with new plan training set");

		Set<ModelSearchNode> toRemove = new LinkedHashSet<ModelSearchNode>();

		for (ModelSearchNode searchModel : open) {

			Model testedModel = unsafeModel.extendModel(searchModel.getTempModel());

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
						toRemove.add(searchModel);
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

	public void FilterMCTSModels(MCTS mcts, ModelSearchNode node, List<StateActionState> planSASList) {

		LOGGER.info("Updating search nodes models with new plan training set");

		if(node.getChildren() == null)
			return;

		Set<ModelSearchNode> toRemove = new LinkedHashSet<ModelSearchNode>();

		for (ModelSearchNode child : node.getChildren()) {

			Model testedModel = unsafeModel.extendModel(child.getTempModel());

			// remove open list models that does not allow OK actions
			for (StateActionState sas : planSASList) {				
				if(!sas.actionOwner.equals(agentName)){

					Action modelAction = testedModel.actions.get(sas.action); 
					Set<String> modelActionPre = new LinkedHashSet<String>(modelAction.preconditions);

					Action safeModelAction = safeModel.actions.get(sas.action); 					
					Set<String> safeModelActionPre = new LinkedHashSet<String>(safeModelAction.preconditions);

					if(!safeModelActionPre.containsAll(modelActionPre)) {
						toRemove.add(child);
					}
				}
			}
		}

		for (ModelSearchNode nodeToRemove : toRemove) {
			mcts.removeNode(nodeToRemove);
		}

		for (ModelSearchNode child : node.getChildren()) {
			FilterMCTSModels(mcts, child, planSASList);
		}
	}

	private Set<ModelSearchNode> ExtendSafe(ModelSearchNode searchNode, StateActionState failedActionSAS) {

		LOGGER.info("Extending the model towards the Safe Model");

		Set<ModelSearchNode> res = new LinkedHashSet<ModelSearchNode>();
		Set<String> preconditions = null;

		if(failedActionSAS == null)
			return res;

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

			TempModel tempModel = new TempModel(searchNode.getTempModel());

			//TempAction tempAction = tempModel.getTempActionByName(failedActionName);
			TempAction tempAction = tempModel.popTempActionByName(failedActionName);

			if(tempAction == null)
				tempAction = new TempAction();

			if(!tempAction.preconditionsAdd.contains(pre) && !tempAction.preconditionsSub.contains(pre)) {

				tempAction.name = failedActionName;

				tempAction.preconditionsAdd.add(pre);

				tempModel.tempActions.add(tempAction);

				ModelSearchNode newSearchNode = new ModelSearchNode(searchNode, tempModel);

				res.add(newSearchNode);
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
		double timeLimitMin = ((double)planningTimeoutInMS)/60000;

		long planningStartTime = System.currentTimeMillis();

		MADLAPlanner planner = new MADLAPlanner(agentDomainPath, agentProblemPath, agentADDLPath,
				heuristic, recursionLevel, timeLimitMin, agentList, agentName);

		List<String> result = planner.plan();

		/*if(planner.isTimeout)
			num_agents_timeout++;

		if(planner.isNotSolved)
			num_agents_not_solved++;*/

		long planningTimeTotal = System.currentTimeMillis() - planningStartTime;

		TestDataAccumulator.getAccumulator().totalPlaningTimeMs += planningTimeTotal;

		Long agentPlanningTimes = TestDataAccumulator.getAccumulator().agentPlanningTimeMs.get(agentName);

		if(agentPlanningTimes == null) 
			agentPlanningTimes = planningTimeTotal;
		else
			agentPlanningTimes += planningTimeTotal;

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

		long verifingStartTime = System.currentTimeMillis();

		long verifingTimeoutMS = (long)timeoutInMS - TestDataAccumulator.getAccumulator().agentPlanningTimeMs.get(agentName) - TestDataAccumulator.getAccumulator().agentLearningTimeMs.get(agentName);

		//verifingTimeoutMS = 5000;

		PlanVerifier planVerifier = new PlanVerifier(agentList,domainFileName,problemFileName,
				verifingTimeoutMS,problemFilesPath, useGrounded);	

		VerificationResult res = planVerifier.verifyPlan(plan,0);

		long verifingTotalTime = System.currentTimeMillis() - verifingStartTime;

		TestDataAccumulator.getAccumulator().totalVerifingTimeMs += verifingTotalTime;

		Long agentVerifingTime = TestDataAccumulator.getAccumulator().agentVerifingTimeMs.get(agentName);

		if(agentVerifingTime == null) 
			agentVerifingTime = verifingTotalTime;
		else
			agentVerifingTime += verifingTotalTime;

		TestDataAccumulator.getAccumulator().agentVerifingTimeMs.put(agentName, agentVerifingTime);

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;	
		}

		return res;
	}

	private PlanToStateActionStateResult planToSASList(List<String> plan, int lastActionIndex) {

		LOGGER.info("generate SAS List from plan");

		long sequancingStartTime = System.currentTimeMillis();

		long planToSASListTimeoutMS = (long)timeoutInMS 
				- TestDataAccumulator.getAccumulator().agentPlanningTimeMs.get(agentName) 
				- TestDataAccumulator.getAccumulator().agentLearningTimeMs.get(agentName) 
				- TestDataAccumulator.getAccumulator().agentVerifingTimeMs.get(agentName);

		//planToSASListTimeoutMS = (long)timeoutInMS - TestDataAccumulator.getAccumulator().getTotalTimeMSforAgent(agentName);

		String problemFilesPath = Globals.INPUT_GROUNDED_PATH;

		PlanToStateActionState plan2SAS = new PlanToStateActionState(domainFileName, problemFileName,
				problemFilesPath,sequancingStartTime, planToSASListTimeoutMS);

		List<StateActionState> planSASList = plan2SAS.generateSASList(plan, lastActionIndex);
		StateActionState failedActionSAS = null;

		if(planSASList == null)
			return new PlanToStateActionStateResult(null, null, true);		

		failedActionSAS = planSASList.get(lastActionIndex);
		planSASList.remove(lastActionIndex);

		sequancingAmountTotal = planSASList.size() + 1;

		long sequancingEndTime = System.currentTimeMillis();

		sequancingTimeTotal = sequancingEndTime - sequancingStartTime;
		//sequancingAmountTotal = planSASList.size() + 1;

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;	
		}

		return new PlanToStateActionStateResult(planSASList, failedActionSAS, false);
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
}
