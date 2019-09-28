package PlannerAndLearner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
import OurPlanner.PlanVerifier;
import OurPlanner.StateActionState;
import OurPlanner.TraceLearner;
import Utils.FileDeleter;
import Utils.VerificationResult;

public class PlannerAndModelLearner {

	private final static Logger LOGGER = Logger.getLogger(PlannerAndModelLearner.class);

	private	Random rnd = new Random();

	private String domainFileName = "";
	private String problemFileName = "";

	private String agentName = "";
	private List<String> agentList = null;

	private TraceLearner learner = null;


	public PlannerAndModelLearner(String agentName, List<String> agentList, String domainFileName,
			String problemFileName, TraceLearner learner) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("PlannerAndModelLearner constructor");

		this.agentName = agentName;
		this.agentList = agentList;
		this.domainFileName = domainFileName;
		this.problemFileName = problemFileName;
		this.learner = learner;

		logInput();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentName: " + agentName);
		LOGGER.info("agentList: " + agentList);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
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


	public boolean test () {

		LOGGER.info("test");

		if(!copyProblemFiles()) {
			LOGGER.info("Coping domain file failure");
			return false;
		}		

		String safeModelPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
		String unsafeModelPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		Model safeModel = new Model();
		Model unsafeModel = new Model();

		if(!safeModel.readModel(safeModelPath)) {
			LOGGER.fatal("provided path to safe model domain file not existing");
			return false;
		}

		if(!unsafeModel.readModel(unsafeModelPath)) {
			LOGGER.fatal("provided path to unsafe model domain file not existing");
			return false;
		}

		TempModel tmpModel  = new TempModel();

		Set<TempModel> open = new LinkedHashSet<TempModel>();
		Set<TempModel> closed = new LinkedHashSet<TempModel>();

		open.add(tmpModel);

		while (!open.isEmpty()) {

			int index = rnd.nextInt(open.size());

			TempModel currTempModel = (TempModel) (open.toArray())[index];

			open.remove(currTempModel);
			closed.add(currTempModel);			

			Model currModel = safeModel.extendModel(currTempModel);

			writeDomainFile(currModel.reconstructModelString());

			List<String> plan = plan();

			if(plan == null) {
				open.addAll(ExtendUnsafe(currModel, unsafeModel, currTempModel));
				open.removeAll(closed);
			}
			else {

				VerificationResult res = verify(plan);

				if(res!=null) {

					if(res.isVerified) {
						LOGGER.info("GREAT!!");
					}
					else {

						List<StateActionState> planSASList = planToSASList(plan, res.lastActionIndex);
						StateActionState failedActionSAS = planSASList.get(res.lastActionIndex);

						planSASList.remove(res.lastActionIndex);						

						UpdateModels(safeModel, unsafeModel,open, closed, planSASList, failedActionSAS);

						if(res.lastActionIndex > 0)
							open.addAll(ExtendSafe(currModel, safeModel, currTempModel, failedActionSAS));

						open.removeAll(closed);
					}
				}
				else
					return false;
			}
		}

		return true;
	}

	private boolean UpdateModels(Model safeModel, Model unsafeModel, Set<TempModel> open, Set<TempModel> closed,
			List<StateActionState> planSASList, StateActionState failedActionSAS) {

		LOGGER.info("Updating safe and unsafe models with new plan training set");

		String safeModelPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
		String unsafeModelPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		learner.learnActionsFromPlan(planSASList,Globals.OUTPUT_SAFE_MODEL_PATH,Globals.OUTPUT_UNSAFE_MODEL_PATH);

		safeModel = new Model();
		unsafeModel = new Model();

		if(!safeModel.readModel(safeModelPath)) {
			LOGGER.fatal("provided path to safe model domain file not existing");
			return false;
		}

		if(!unsafeModel.readModel(unsafeModelPath)) {
			LOGGER.fatal("provided path to unsafe model domain file not existing");
			return false;
		}

		return true;
	}

	private Set<TempModel> ExtendSafe(Model currModel, Model safeModel, TempModel currTempModel,
			StateActionState failedActionSAS) {

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

			if(!tempAction.preconditionsSub.contains(pre)) {

				tempAction.name = failedActionName;

				tempAction.preconditionsAdd.add(pre);

				tempModel.tempActions.add(tempAction);

				res.add(tempModel);
			}
		}

		return res;
	}

	private Set<String> formatFacts(Set<String> statePreconditions) {

		Set<String> res = new LinkedHashSet<String>();

		for (String fact : statePreconditions) {

			String formattedFact = fact;

			formattedFact = formattedFact.replace("(", " ");
			formattedFact = formattedFact.replace(",", "");
			formattedFact = formattedFact.replace(")", "");

			formattedFact = '(' + formattedFact + ')';

			res.add(formattedFact);
		}

		return res;
	}

	private Set<TempModel> ExtendUnsafe(Model currModel, Model unsafeModel,
			TempModel currTempModel) {

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
				
				if(!tempAction.preconditionsSub.contains(pre)) {

					tempAction.name = action.name;

					tempAction.preconditionsSub.add(pre);

					tempModel.tempActions.add(tempAction);

					res.add(tempModel);
				}

			}
		}

		return res;
	}

	private List<String> plan() {

		LOGGER.info("Planning for agent");

		String agentDomainPath = Globals.OUTPUT_SOUND_MODEL_PATH + "/" + domainFileName;
		String agentProblemPath = Globals.OUTPUT_SOUND_MODEL_PATH + "/" + problemFileName;
		String agentADDLPath = Globals.OUTPUT_TEMP_PATH + "/" + problemFileName.split("\\.")[0] + ".addl";

		String heuristic = "saFF-glcl";
		int recursionLevel = -1;
		double timeLimitMin = 0.1666;

		MADLAPlanner planner = new MADLAPlanner(agentDomainPath, agentProblemPath, agentADDLPath,
				heuristic, recursionLevel, timeLimitMin, agentList, agentName);

		List<String> result = planner.plan();

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

		String problemFilesPath = Globals.INPUT_GROUNDED_PATH;

		PlanToStateActionState plan2SAS = new PlanToStateActionState(domainFileName, problemFileName, problemFilesPath);

		List<StateActionState> res = plan2SAS.generateSASList(plan, lastActionIndex);

		if(!FileDeleter.deleteTempFiles()) {
			LOGGER.info("Deleting Temporary files failure");
			return null;	
		}

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
}
