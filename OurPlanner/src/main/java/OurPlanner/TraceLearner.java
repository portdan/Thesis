package OurPlanner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import cz.agents.dimaptools.model.Domain;

public class TraceLearner {

	private final static Logger LOGGER = Logger.getLogger(TraceLearner.class);

	private List<String> agentList = null;

	private File trajectoryFiles = null;
	private File problemFiles = null;
	private File localViewFiles = null;

	private String domainFileName = "";
	private String problemFileName = "";

	private int numOfTracesToUse = 0;
	private int tracesLearinigInterval = 0;

	Map<String, Set<String>> agentLearnedActionNames = new HashMap<String,Set<String>>();

	Map<String, Long> agentLearningTimes = new HashMap<String, Long>();

	Map<String, Set<String>> agentLearnedSafeActionsPreconditions = new HashMap<String,Set<String>>();
	Map<String, Set<String>> agentLearnedSafeActionsEffects = new HashMap<String,Set<String>>();
	Map<String, Set<String>> agentLearnedUnSafeActionsPreconditions = new HashMap<String,Set<String>>();
	Map<String, Set<String>> agentLearnedUnSafeActionsEffects = new HashMap<String,Set<String>>();

	StateActionStateSequencer sasSequencer = null;
	DeleteEffectGenerator DEGenerator = null;

	public TraceLearner(List<String> agentList , String agentName, File trajectoryFiles ,
			File problemFiles , File localViewFiles ,String domainFileName ,String problemFileName, 
			int numOfTracesToUse, int tracesLearinigInterval) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("TraceLearner constructor");

		this.agentList = agentList;
		this.trajectoryFiles = trajectoryFiles;
		this.problemFiles = problemFiles;
		this.localViewFiles = localViewFiles;
		this.domainFileName = new String(domainFileName);
		this.problemFileName = new String(problemFileName);
		this.numOfTracesToUse = numOfTracesToUse;
		this.tracesLearinigInterval = tracesLearinigInterval;

		for (String agent : agentList) 
			agentLearningTimes.put(agent,(long) 0);

		logInput();
	}

	public TraceLearner(List<String> agentList, File trajectoryFiles , File problemFiles , File localViewFiles, 
			String domainFileName ,String problemFileName, int numOfTracesToUse, int tracesLearinigInterval) {

		this(agentList, "", trajectoryFiles, problemFiles, localViewFiles, domainFileName, problemFileName, 
				numOfTracesToUse, tracesLearinigInterval);
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentList: " + agentList);
		LOGGER.info("trajectoryFiles: " + trajectoryFiles);
		LOGGER.info("problemFiles: " + problemFiles);
		LOGGER.info("localViewFiles: " + localViewFiles);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("numOfTracesToUse: " + numOfTracesToUse);
		LOGGER.info("tracesLearinigInterval: " + tracesLearinigInterval);

	}

	public boolean learnNewActions() {

		LOGGER.info("Learning new actions");

		TestDataAccumulator.getAccumulator().initialTrainingSize = 0;
		
		sasSequencer = new StateActionStateSequencer(agentList, 
				problemFiles, domainFileName, problemFileName, trajectoryFiles);

		DEGenerator = new DeleteEffectGenerator (problemFiles,
				domainFileName, problemFileName);

		while(!sasSequencer.StopSequencing) {

			long sequancingStartTime = System.currentTimeMillis();

			List<StateActionState> sasList = sasSequencer.generateSequencesFromSASTraces(numOfTracesToUse, tracesLearinigInterval);

			long sequancingEndTime = System.currentTimeMillis();

			long sequancingTimeTotal = sequancingEndTime - sequancingStartTime;

			long sequancingAmountTotal = sasList.size();

			TestDataAccumulator.getAccumulator().initialTrainingSize += sequancingAmountTotal;			

			while(!sasList.isEmpty())
			{	
				long learningStartTime = System.currentTimeMillis();

				StateActionState firstValue = (StateActionState)sasList.get(0);
				String actionName = firstValue.action;
				String actionOwnerName = firstValue.actionOwner;

				LOGGER.info("Learning action: " + actionName);

				List<StateActionState> sasListForAction = getAllStateActionStateWithAction(sasList, actionName);	

				addActionToOwnerLink(actionName, actionOwnerName);

				learnPreconditionAndEffects(DEGenerator, actionName, sasListForAction);

				long learningFinishTime = System.currentTimeMillis();

				addAgentLearningTime(sequancingTimeTotal, sequancingAmountTotal, learningStartTime,
						actionOwnerName, sasListForAction, learningFinishTime);

				sasList.removeAll(sasListForAction);
			}
		}

		return writeNewSafeLearnedProblemFiles() && writeNewUnSafeLearnedProblemFiles();
	}

	public boolean learnActionsFromPlan(List<StateActionState> planSASList, String pathToSafeModel,
			String pathUnSafeModel, long sequancingTimeTotal, long sequancingAmountTotal) {

		LOGGER.info("Learning new actions from plan");

		TestDataAccumulator.getAccumulator().addedTrainingSize += planSASList.size();

		DEGenerator = new DeleteEffectGenerator (problemFiles,
				domainFileName, problemFileName);

		List<StateActionState> sasList = new ArrayList<StateActionState>(planSASList); 

		while(!sasList.isEmpty()) {

			long learningStartTime = System.currentTimeMillis();

			StateActionState firstValue = (StateActionState)sasList.get(0);

			String actionName = firstValue.action;
			String actionOwnerName = firstValue.actionOwner;

			LOGGER.info("Learning action: " + actionName);

			List<StateActionState> sasListForAction = getAllStateActionStateWithAction(sasList, actionName);	

			addActionToOwnerLink(actionName, actionOwnerName);

			learnPreconditionAndEffects(DEGenerator, actionName, sasListForAction);

			long learningFinishTime = System.currentTimeMillis();

			addAgentLearningTime(sequancingTimeTotal, sequancingAmountTotal, learningStartTime,
					actionOwnerName, sasListForAction, learningFinishTime);

			sasList.removeAll(sasListForAction);

		}

		return writeNewSafeLearnedProblemFiles() && writeNewUnSafeLearnedProblemFiles();
	}

	private void addAgentLearningTime(long sequancingTimeTotal, long sequancingAmountTotal, long learningStartTime,
			String actionOwnerName, List<StateActionState> sasListForAction, long learningFinishTime) {

		LOGGER.info("Linking agent learning times");

		long relativeSequancingTime = (sasListForAction.size()*sequancingTimeTotal) / sequancingAmountTotal;
		long relativeLeariningTime = learningFinishTime - learningStartTime;

		Long currentLeariningTime  = agentLearningTimes.get(actionOwnerName);
		long addedLeariningTime = 0;

		if(currentLeariningTime == null)
			addedLeariningTime = relativeLeariningTime + relativeSequancingTime;
		else
			addedLeariningTime = currentLeariningTime + relativeLeariningTime + relativeSequancingTime;

		agentLearningTimes.put(actionOwnerName, addedLeariningTime);

		TestDataAccumulator.getAccumulator().totalLearningTimeMs += relativeLeariningTime + relativeSequancingTime;
	}

	private void addActionToOwnerLink(String actionName, String actionOwnerName) {

		LOGGER.info("Linking actionName to actionOwner");

		Set<String> agentActions = agentLearnedActionNames.get(actionOwnerName);

		if(agentActions == null) {
			agentActions = new LinkedHashSet<String>();
			agentLearnedActionNames.put(actionOwnerName, agentActions);
		}

		agentActions.add(actionName);
	}

	private void learnPreconditionAndEffects(DeleteEffectGenerator DEGenerator, String actionName, 
			List<StateActionState> sasListForAction) {

		// SAFE MODEL //
		LOGGER.info("Learning safe preconditions and effects");

		Set<String> safePre = learnSafePreconditions(sasListForAction);
		//Set<String> safeEff = learnSafeEffects(sasListForAction);
		Set<String> safeEff = DEGenerator.generateAllEffects(actionName);

		safeEff.addAll(DEGenerator.generateDeleteEffects(actionName,safePre,safeEff));

		safePre = formatFacts(safePre);
		safeEff = formatFacts(safeEff);

		Set<String> safePreSet = agentLearnedSafeActionsPreconditions.get(actionName);

		if(safePreSet == null) {
			safePreSet = new HashSet<String>();
			safePreSet.addAll(safePre);
			agentLearnedSafeActionsPreconditions.put(actionName, safePreSet);
		}

		safePreSet.retainAll(safePre);

		Set<String> safeEffSet = agentLearnedSafeActionsEffects.get(actionName);

		if(safeEffSet == null) {
			safeEffSet = new HashSet<String>();
			safeEffSet.addAll(safeEff);
			agentLearnedSafeActionsEffects.put(actionName, safeEffSet);
		}

		safeEffSet.addAll(safeEff);
		// SAFE MODEL //


		// UNSAFE MODEL //
		LOGGER.info("Learning unsafe preconditions and effects");

		Set<String> unsafePre = learnUnSafePreconditions(sasListForAction);
		//Set<String> unsafeEff = learnUnSafeEffects(sasListForAction);
		Set<String> unsafeEff = DEGenerator.generateAllEffects(actionName);

		unsafeEff.addAll(DEGenerator.generateDeleteEffects(actionName,unsafePre,unsafeEff));

		unsafePre = formatFacts(unsafePre);
		unsafeEff = formatFacts(unsafeEff);

		Set<String> unSafePreSet = agentLearnedUnSafeActionsPreconditions.get(actionName);

		if(unSafePreSet == null) {
			unSafePreSet = new HashSet<String>();
			unSafePreSet.addAll(unsafePre);
			agentLearnedUnSafeActionsPreconditions.put(actionName, unSafePreSet);
		}

		unSafePreSet.addAll(unsafePre);

		Set<String> unSafeEffSet = agentLearnedUnSafeActionsEffects.get(actionName);

		if(unSafeEffSet == null) {
			unSafeEffSet = new HashSet<String>();
			unSafeEffSet.addAll(unsafeEff);
			agentLearnedUnSafeActionsEffects.put(actionName, unSafeEffSet);
		}

		unSafeEffSet.retainAll(unsafeEff);
		// UNSAFE MODEL //
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

			if(formattedFact.startsWith(Globals.NONE_KEYWORD)) {
				//formatted.addAll(formatNONEFact(formattedFact, isNegated));
				//formatted.add(formattedFact);
			}
			else {
				if (isNegated)
					formattedFact = "not (" + formattedFact + ")";

				formatted.add(formattedFact);
			}
		}

		return formatted;
	}

	private Set<String> formatNONEFact(String fact, boolean isNegated) {

		Set<String> formatted = new HashSet<String>();

		if(isNegated)
			return formatted;

		String var = fact.split("-")[1];	

		Map<Integer, Set<Integer>> variableDomains = DEGenerator.problem.getDomain().getVariableDomains();

		Map<String, Integer> valCodes =  DEGenerator.preprocessor.valCodes;
		Map<String, Integer> varCodes =  DEGenerator.preprocessor.varCodes;

		int varCode = varCodes.get(var);
		int valCode = valCodes.get(fact);

		for (int val : variableDomains.get(varCode)) {

			if(valCode!=val)
				formatted.add("not (" + Domain.valNames.get(val).toString() + ")");
		}


		return formatted;
	}

	private boolean writeNewSafeLearnedProblemFiles() {

		LOGGER.info("writing newly safe learned problem .pddl file");

		for (String agentName : agentList) {

			String domainPath = localViewFiles + "/" + agentName + "/" + domainFileName;

			String domainStr = readDomainString(domainPath, agentName);

			if(domainStr.isEmpty())
			{
				LOGGER.info("Reading domain pddl failure");
				return false;
			}

			String learnedSafeActionsString = generateLearnedSafeActionsString(agentName);

			/*if(learnedSafeActionsString.isEmpty())
			{
				LOGGER.info("Reading domain pddl failure");
				return false;
			}*/

			String learnedDomainStr = addActionsToDomainString(domainStr, learnedSafeActionsString);

			/*if(domainStr.isEmpty())
			{
				LOGGER.info("Adding new actions to domain failure");
				return false;
			}*/

			String learnedDomainPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

			if(!writeNewSafeDomain(learnedDomainStr, agentName, learnedDomainPath)) {
				LOGGER.info("Writing new safe domain to file failure");
				return false;
			}
		}

		return true;
	}

	private boolean writeNewUnSafeLearnedProblemFiles() {

		LOGGER.info("writing newly unsafe learned problem .pddl file");


		for (String agentName : agentList) {

			String domainPath = localViewFiles + "/" + agentName + "/" + domainFileName;

			String domainStr = readDomainString(domainPath, agentName);

			if(domainStr.isEmpty())
			{
				LOGGER.info("Reading domain pddl failure");
				return false;
			}

			String learnedUnSafeActionsString = generateLearnedUnSafeActionsString(agentName);

			/*if(learnedUnSafeActionsString.isEmpty())
			{
				LOGGER.info("Reading domain pddl failure");
				return false;
			}
			 */
			String learnedDomainStr = addActionsToDomainString(domainStr, learnedUnSafeActionsString);

			/*if(domainStr.isEmpty())
			{
				LOGGER.info("Adding new actions to domain failure");
				return false;
			}*/

			String learnedDomainPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

			if(!writeNewUnSafeDomain(learnedDomainStr, agentName, learnedDomainPath)) {
				LOGGER.info("Writing new unsafe domain to file failure");
				return false;
			}
		}

		return true;
	}

	private String generateLearnedSafeActionsString(String agentName) {

		LOGGER.info("Generating safe actions string");

		Iterator<Entry<String, Set<String>>> it = agentLearnedActionNames.entrySet().iterator();

		StringBuilder sb = new StringBuilder();

		while (it.hasNext()) {
			Entry<String, Set<String>> agentActionNames = (Entry<String, Set<String>>)it.next();

			Set<String> actionNames = agentActionNames.getValue();
			String actionOwnerName = agentActionNames.getKey();

			for (String actionName : actionNames) {			
				if(!actionOwnerName.equals(agentName))
					sb.append(generateSafeAction(agentName, actionName));
			}
		}

		return sb.toString();
	}

	private String generateLearnedUnSafeActionsString(String agentName) {

		LOGGER.info("Generating unsafe actions string");

		Iterator<Entry<String, Set<String>>> it = agentLearnedActionNames.entrySet().iterator();

		StringBuilder sb = new StringBuilder();

		while (it.hasNext()) {
			Entry<String, Set<String>> agentActionNames = (Entry<String, Set<String>>)it.next();

			Set<String> actionNames = agentActionNames.getValue();
			String actionOwnerName = agentActionNames.getKey();

			for (String actionName : actionNames) {			
				if(!actionOwnerName.equals(agentName))
					sb.append(generateUnSafeAction(agentName, actionName));
			}
		}

		return sb.toString();
	}

	private String generateSafeAction(String agentName, String actionName) {

		LOGGER.info("Generating safe action");

		Set<String> safePreSet = agentLearnedSafeActionsPreconditions.get(actionName);
		Set<String> safeEffSet = agentLearnedSafeActionsEffects.get(actionName);

		return generateLearnedAction(safePreSet, safeEffSet, actionName, agentName);
	}

	private String generateUnSafeAction(String agentName, String actionName) {

		LOGGER.info("Generating unsafe action");

		Set<String> unsafePreSet = agentLearnedUnSafeActionsPreconditions.get(actionName);
		Set<String> unsafeEffSet = agentLearnedUnSafeActionsEffects.get(actionName);

		return generateLearnedAction(unsafePreSet, unsafeEffSet, actionName, agentName);
	}

	private boolean writeNewSafeDomain(String newDomainString, String agentName, String outputPath) {

		LOGGER.info("Writing new safe PDDL domain file");

		try {
			FileUtils.writeStringToFile(new File(outputPath), newDomainString, Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return false;
		}

		return true;
	}

	private boolean writeNewUnSafeDomain(String newDomainString, String agentName, String outputPath) {

		LOGGER.info("Writing new unsafe PDDL domain file");

		try {
			FileUtils.writeStringToFile(new File(outputPath), newDomainString, Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return false;
		}

		return true;
	}

	private String addActionsToDomainString(String domainString, String actionsString) {

		LOGGER.info("Adding new actions to domain string");

		StringBuilder sb = new StringBuilder(domainString);

		int end = sb.lastIndexOf(")");

		if(end == -1)
			return "";
		else {
			sb.insert(end, actionsString);
			end += actionsString.length();
		}

		return sb.toString();
	}

	private String readDomainString(String domainPath,String agentName) {

		LOGGER.info("Reading domain pddl string");

		String fileStr = "";

		try {
			fileStr = FileUtils.readFileToString(new File(domainPath),Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return "";
		}

		return fileStr;
	}

	private String generateLearnedAction(Set<String> pre, Set<String> eff, String action, String actionOwner) {

		LOGGER.info("Building action string");

		String rep = "";

		String actionName = action.replace(" ",Globals.PARAMETER_INDICATION);
		//String actionName = action;

		rep += "(:action " + actionName + "\n";
		rep += "\t:agent ?" + actionOwner + " - " + actionOwner + "\n";
		rep += "\t:parameters ()\n";

		if (pre.size() > 1)
			rep += "\t:precondition (and\n";
		else
			rep += "\t:precondition \n";
		if(pre.isEmpty())
			rep += "\t\t()\n";
		else
			for (String p : pre)
				rep += "\t\t(" + p + ")\n";
		if (pre.size() > 1)
			rep += "\t)\n";
		if (eff.size() > 1)
			rep += "\t:effect (and\n";
		else
			rep += "\t:effect \n";
		if(eff.isEmpty())
			rep += "\t\t()\n";
		else
			for (String e : eff) 
				rep += "\t\t(" + e + ")\n";
		if (eff.size() > 1)
			rep += "\t)\n";
		rep += ")\n";

		return rep;
	}

	private Set<String> learnSafePreconditions(List<StateActionState> sasList) {

		LOGGER.info("Learning safe preconditions");

		Set<String> res = new HashSet<String>();

		res.addAll(sasList.get(0).pre);

		for (StateActionState sas : sasList) {
			res.retainAll(sas.pre);
		}

		return res;
	}

	private Set<String> learnUnSafePreconditions(List<StateActionState> sasList) {

		LOGGER.info("Learning unsafe preconditions");

		Set<String> res = new HashSet<String>();

		return res;
	}

	private Set<String> learnSafeEffects(List<StateActionState> sasList) {

		LOGGER.info("Learning safe effects");

		Set<String> res = new HashSet<String>();

		for (StateActionState sas : sasList) {

			Set<String> preFacts = new HashSet<String>(sas.pre);
			Set<String> postFacts = new HashSet<String>(sas.post);

			postFacts.removeAll(preFacts);

			res.addAll(postFacts);		
		}

		return res;
	}

	private Set<String> learnUnSafeEffects(List<StateActionState> sasList) {

		LOGGER.info("Learning unsafe effects");

		Set<String> res = new HashSet<String>();

		res.addAll(sasList.get(0).post);

		for (StateActionState sas : sasList) {
			res.retainAll(sas.post);
		}

		return res;
	}

	private List<StateActionState> getAllStateActionStateWithAction(List<StateActionState> trajectorySequences, String action) {

		LOGGER.info("Getting all StateActionState's with action " + action);

		List<StateActionState> res = new ArrayList<StateActionState>();

		for (StateActionState sas: trajectorySequences) {
			if(sas.action.equals(action))
				res.add(sas);
		}

		return res;
	}
}