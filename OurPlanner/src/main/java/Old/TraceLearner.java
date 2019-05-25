package Old;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import OurPlanner.*;
import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.State;

public class TraceLearner {

	private final static Logger LOGGER = Logger.getLogger(TraceLearner.class);

	private List<String> agentList = null;
	private String agentName = "";

	private File trajectoryFiles = null;
	private File problemFiles = null;
	private File localViewFiles = null;

	private String domainFileName = "";
	private String problemFileName = "";

	private int numOfTracesToUse = 0;
	private int tracesLearinigInterval = 0;

	Map<String, List<String>> agentLearnedSafeActions = new HashMap<String,List<String>>();
	Map<String, List<String>> agentLearnedUnSafeActions = new HashMap<String,List<String>>();

	Map<String, List<String>> agentLearnedActionNames = new HashMap<String,List<String>>();

	Map<String, Long> agentLearningTimes = new HashMap<String, Long>();

	Map<String, Set<String>> agentLearnedSafeActionsPreconditions = new HashMap<String,Set<String>>();
	Map<String, Set<String>> agentLearnedSafeActionsEffects = new HashMap<String,Set<String>>();
	Map<String, Set<String>> agentLearnedUnSafeActionsPreconditions = new HashMap<String,Set<String>>();
	Map<String, Set<String>> agentLearnedUnSafeActionsEffects = new HashMap<String,Set<String>>();

	Set<String> ActionNames = new HashSet<String>();

	public TraceLearner(List<String> agentList , String agentName, File trajectoryFiles ,
			File problemFiles , File localViewFiles ,String domainFileName ,String problemFileName, 
			int numOfTracesToUse, int tracesLearinigInterval) {

		LOGGER.setLevel(Level.INFO);

		LOGGER.info("TraceLearner constructor");

		this.agentName = agentName;
		this.agentList = agentList;
		this.trajectoryFiles = trajectoryFiles;
		this.problemFiles = problemFiles;
		this.localViewFiles = localViewFiles;
		this.domainFileName = new String(domainFileName);
		this.problemFileName = new String(problemFileName);
		this.numOfTracesToUse = numOfTracesToUse;
		this.tracesLearinigInterval = tracesLearinigInterval;

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
		LOGGER.info("agentName: " + agentName);
		LOGGER.info("trajectoryFiles: " + trajectoryFiles);
		LOGGER.info("problemFiles: " + problemFiles);
		LOGGER.info("localViewFiles: " + localViewFiles);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("numOfTracesToUse: " + numOfTracesToUse);
		LOGGER.info("tracesLearinigInterval: " + tracesLearinigInterval);

	}

	/*
	 public boolean learnNewActions() {

		LOGGER.info("Learning new actions");

		List<String> learnedActions = new ArrayList<String>();

		StateActionStateSequencer sasSequencer = new StateActionStateSequencer(agentList, 
				problemFiles, domainFileName, problemFileName, trajectoryFiles);

		DeleteEffectGenerator DEGenerator = new DeleteEffectGenerator (problemFiles,
				domainFileName, problemFileName);

		//List<StateActionState> trajectorySequences = sasSequencer.generateSequences();

		List<StateActionState> trajectorySequences = sasSequencer.generateSequencesFromSASTraces(numOfTracesToUse);

		while(!trajectorySequences.isEmpty()) {

			StateActionState firstValue = (StateActionState)trajectorySequences.get(0);

			//LOGGER.info("Learning action: " + getActionName(firstValue.action));

			LOGGER.info("Learning action: " + firstValue.action);

			List<StateActionState> sasList = getAllStateActionStateWithAction(trajectorySequences, firstValue.action);

			if(!firstValue.actionOwner.equals(agentName)) {

				Set<String> pre = learnPreconditions(sasList);
				Set<String> eff = learnEffects(sasList);

				eff.addAll(DEGenerator.generateDeleteEffects(firstValue.action,pre,eff));

				pre = FormatFacts(pre);
				eff = FormatFacts(eff);

				learnedActions.add(generateLearnedAction(pre,eff,firstValue.action,firstValue.actionOwner));
			}

			trajectorySequences.removeAll(sasList);
		}

		return writeNewLearnedProblemFiles(learnedActions);
	}

	 */


	/*
	public boolean learnNewActions() {

		LOGGER.info("Learning new actions");

		TestDataAccumulator.getAccumulator().trainingSize = 0;

		StateActionStateSequencer sasSequencer = new StateActionStateSequencer(agentList, problemFiles, 
				domainFileName, problemFileName, trajectoryFiles);

		DeleteEffectGenerator DEGenerator = new DeleteEffectGenerator (problemFiles,
				domainFileName, problemFileName);

		//List<StateActionState> trajectorySequences = sasSequencer.generateSequences();

		for (String agentName : agentList) {

			long learningStartTime = System.currentTimeMillis();

			List<String> learnedSafeActions = new ArrayList<String>();
			List<String> learnedUnSafeActions = new ArrayList<String>();

			sasSequencer.setSequencingData(agentName);

			while(!sasSequencer.fileEnd) {

				List<StateActionState> sasList = sasSequencer.generateSASListForAction(numOfTracesToUse);

				if(!sasList.isEmpty())
				{	
					StateActionState firstValue = (StateActionState)sasList.get(0);

					//LOGGER.info("Learning action: " + getActionName(firstValue.action));

					LOGGER.info("Learning action: " + firstValue.action);

					// SAFE MODEL //
					Set<String> safePre = learnSafePreconditions(sasList);
					Set<String> safeEff = learnSafeEffects(sasList);

					safeEff.addAll(DEGenerator.generateDeleteEffects(firstValue.action,safePre,safeEff));

					safePre = FormatFacts(safePre);
					safeEff = FormatFacts(safeEff);

					learnedSafeActions.add(generateLearnedAction(safePre,safeEff,firstValue.action,firstValue.actionOwner));					
					// SAFE MODEL //


					// UNSAFE MODEL //
					Set<String> unsafePre = learnUnSafePreconditions(sasList);
					Set<String> unsafeEff = learnUnSafeEffects(sasList);

					unsafeEff.addAll(DEGenerator.generateDeleteEffects(firstValue.action,unsafePre,unsafeEff));

					unsafePre = FormatFacts(unsafePre);
					unsafeEff = FormatFacts(unsafeEff);

					learnedUnSafeActions.add(generateLearnedAction(unsafePre,unsafeEff,firstValue.action,firstValue.actionOwner));					
					// UNSAFE MODEL //
				}
			}

			long learningFinishTime = System.currentTimeMillis();

			TestDataAccumulator.getAccumulator().totalLearningTimeMs += learningFinishTime - learningStartTime;
			TestDataAccumulator.getAccumulator().agentLearningTimeMs.put(agentName, learningFinishTime - learningStartTime);

			agentLearningTimes.put(agentName, learningFinishTime - learningStartTime);

			agentLearnedSafeActions.put(agentName, learnedSafeActions);
			agentLearnedUnSafeActions.put(agentName, learnedUnSafeActions);

		}


//		for (String agentName : agentList) {
//
//			long agentLearningTime = 0;
//
//			for (String otherAgentName : agentList) 
//				if(!otherAgentName.equals(agentName))
//					agentLearningTime += agentLearningTimes.get(otherAgentName);
//
//			TestDataAccumulator.getAccumulator().agentLearningTimeMs.put(agentName, agentLearningTime);
//
//			TestDataAccumulator.getAccumulator().totalLearningTimeMs += agentLearningTimes.get(agentName);
//		}


		return writeNewLearnedProblemFiles();
	}
	 */

	public boolean learnNewActions() {

		LOGGER.info("Learning new actions");

		TestDataAccumulator.getAccumulator().trainingSize = 0;

		StateActionStateSequencer sasSequencer = new StateActionStateSequencer(agentList, 
				problemFiles, domainFileName, problemFileName, trajectoryFiles);

		DeleteEffectGenerator DEGenerator = new DeleteEffectGenerator (problemFiles,
				domainFileName, problemFileName);

		while(!sasSequencer.StopSequencing) {

			long sequancingStartTime = System.currentTimeMillis();

			List<StateActionState> sasList = sasSequencer.generateSequencesFromSASTraces(numOfTracesToUse, tracesLearinigInterval);

			long sequancingEndTime = System.currentTimeMillis();

			long sequancingTimeTotal = sequancingEndTime - sequancingStartTime;

			long sequancingTotal = sasList.size();

			while(!sasList.isEmpty())
			{	
				long learningStartTime = System.currentTimeMillis();

				StateActionState firstValue = (StateActionState)sasList.get(0);
				String actionName = firstValue.action;
				String actionOwnerName = firstValue.actionOwner;

				LOGGER.info("Learning action: " + actionName);

				List<StateActionState> sasListForAction = getAllStateActionStateWithAction(sasList, actionName);	

				List<String> agentActions = agentLearnedActionNames.get(actionOwnerName);

				if(agentActions == null) {
					agentActions = new ArrayList<String>();
					agentLearnedActionNames.put(actionOwnerName, agentActions);
				}

				agentActions.add(actionName);

				// SAFE MODEL //
				Set<String> safePre = learnSafePreconditions(sasListForAction);
				Set<String> safeEff = learnSafeEffects(sasListForAction);

				safeEff.addAll(DEGenerator.generateDeleteEffects(actionName,safePre,safeEff));

				safePre = FormatFacts(safePre);
				safeEff = FormatFacts(safeEff);

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
				Set<String> unsafePre = learnUnSafePreconditions(sasListForAction);
				Set<String> unsafeEff = learnUnSafeEffects(sasListForAction);

				unsafeEff.addAll(DEGenerator.generateDeleteEffects(actionName,unsafePre,unsafeEff));

				unsafePre = FormatFacts(unsafePre);
				unsafeEff = FormatFacts(unsafeEff);

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

				long learningFinishTime = System.currentTimeMillis();

				long relativeSequancingTime = (sasListForAction.size()*sequancingTimeTotal) / sequancingTotal;
				long relativeLeariningTime = learningFinishTime - learningStartTime;

				Long currentLeariningTime  = agentLearningTimes.get(actionOwnerName);

				if(currentLeariningTime == null)
					agentLearningTimes.put(actionOwnerName, relativeLeariningTime + relativeSequancingTime);
				else
					agentLearningTimes.put(actionOwnerName, currentLeariningTime + relativeLeariningTime + relativeSequancingTime);

				sasList.removeAll(sasListForAction);
			}
		}

		for (String agentName : agentList) {

			long agentLearningTime = 0;

			for (String otherAgentName : agentList) 
				if(!otherAgentName.equals(agentName))
					agentLearningTime += agentLearningTimes.get(otherAgentName);

			TestDataAccumulator.getAccumulator().agentLearningTimeMs.put(agentName, agentLearningTime);
		}

		return writeNewSafeLearnedProblemFiles() && writeNewUnSafeLearnedProblemFiles();
	}

	private Set<String> FormatFacts(Set<String> facts) {

		Set<String> formatted = new HashSet<String>();

		for (String fact : facts) {

			int startIndex = 0;
			int endIndex = fact.length();

			boolean isNegated = false;

			if(fact.startsWith("not")) {
				isNegated = true;
				startIndex = fact.indexOf('(');
				endIndex = fact.lastIndexOf(')');
			}

			String formattedFact = fact.substring(startIndex,endIndex);

			formattedFact = formattedFact.replace("(", " ");
			formattedFact = formattedFact.replace(",", "");
			formattedFact = formattedFact.replace(")", "");

			formattedFact = formattedFact.trim();

			if (isNegated) {
				formattedFact = "not (" + formattedFact + ")";
			}

			formatted.add(formattedFact);
		}

		return formatted;
	}

	/*
	private boolean writeNewLearnedProblemFiles(List<String> learnedActions) {

		LOGGER.info("writing newly learned problem .pddl files");

		String domainStr = readDomainString();

		if(domainStr.isEmpty())
		{
			LOGGER.info("Reading domain pddl failure");
			return false;
		}

		domainStr = addActionsToDomainString(learnedActions,domainStr ,agentName);

		if(domainStr.isEmpty())
		{
			LOGGER.info("Adding new actions to domain failure");
			return false;
		}

		if(!writeNewDomain(domainStr)) {
			LOGGER.info("Writing new domain to file failure");
			return false;
		}

		return !learnedActions.isEmpty();
	}
	 */

	private boolean writeNewSafeLearnedProblemFiles() {

		LOGGER.info("writing newly safe learned problem .pddl file");

		/*
		File safeActionsFile = new File(Globals.SAFE_MODEL_PATH + "/tmp.txt");
		File unsafeActionsFile = new File(Globals.UNSAFE_MODEL_PATH + "/tmp.txt");

		if(writeLearnedActionsTMPfile(safeActionsFile, unsafeActionsFile))
		{
			LOGGER.info("writing tmp actions failure!");
			return false;
		}		
		 */

		for (String agentName : agentList) {

			String domainStr = readDomainString(agentName);

			if(domainStr.isEmpty())
			{
				LOGGER.info("Reading domain pddl failure");
				return false;
			}

			String learnedSafeActionsString = generateLearnedSafeActionsString(agentName);

			if(learnedSafeActionsString.isEmpty())
			{
				LOGGER.info("Reading domain pddl failure");
				return false;
			}

			String learnedDomainStr = addActionsToDomainString(domainStr, learnedSafeActionsString);

			if(domainStr.isEmpty())
			{
				LOGGER.info("Adding new actions to domain failure");
				return false;
			}

			if(!writeNewSafeDomain(learnedDomainStr, agentName)) {
				LOGGER.info("Writing new safe domain to file failure");
				return false;
			}
		}

		return true;
	}

	private boolean writeNewUnSafeLearnedProblemFiles() {

		LOGGER.info("writing newly unsafe learned problem .pddl file");

		/*
		File safeActionsFile = new File(Globals.SAFE_MODEL_PATH + "/tmp.txt");
		File unsafeActionsFile = new File(Globals.UNSAFE_MODEL_PATH + "/tmp.txt");

		if(writeLearnedActionsTMPfile(safeActionsFile, unsafeActionsFile))
		{
			LOGGER.info("writing tmp actions failure!");
			return false;
		}		
		 */

		for (String agentName : agentList) {

			String domainStr = readDomainString(agentName);

			if(domainStr.isEmpty())
			{
				LOGGER.info("Reading domain pddl failure");
				return false;
			}

			String learnedUnSafeActionsString = generateLearnedUnSafeActionsString(agentName);

			if(learnedUnSafeActionsString.isEmpty())
			{
				LOGGER.info("Reading domain pddl failure");
				return false;
			}

			String learnedDomainStr = addActionsToDomainString(domainStr, learnedUnSafeActionsString);

			if(domainStr.isEmpty())
			{
				LOGGER.info("Adding new actions to domain failure");
				return false;
			}

			if(!writeNewUnSafeDomain(learnedDomainStr, agentName)) {
				LOGGER.info("Writing new unsafe domain to file failure");
				return false;
			}
		}

		return true;
	}

	private String generateLearnedSafeActionsString(String agentName) {

		Iterator<Entry<String, List<String>>> it = agentLearnedActionNames.entrySet().iterator();

		StringBuilder sb = new StringBuilder();

		while (it.hasNext()) {
			Entry<String, List<String>> agentActionNames = (Entry<String, List<String>>)it.next();

			List<String> actionNames = agentActionNames.getValue();
			String actionOwnerName = agentActionNames.getKey();

			for (String actionName : actionNames) {			
				if(!actionOwnerName.equals(agentName))
					sb.append(generateSafeAction(agentName, actionName));
			}
		}

		return sb.toString();
	}

	private String generateLearnedUnSafeActionsString(String agentName) {

		Iterator<Entry<String, List<String>>> it = agentLearnedActionNames.entrySet().iterator();

		StringBuilder sb = new StringBuilder();

		while (it.hasNext()) {
			Entry<String, List<String>> agentActionNames = (Entry<String, List<String>>)it.next();

			List<String> actionNames = agentActionNames.getValue();
			String actionOwnerName = agentActionNames.getKey();

			for (String actionName : actionNames) {			
				if(!actionOwnerName.equals(agentName))
					sb.append(generateUnSafeAction(agentName, actionName));
			}
		}

		return sb.toString();
	}

	private String generateSafeAction(String agentName, String actionName) {

		Set<String> safePreSet = agentLearnedSafeActionsPreconditions.get(actionName);
		Set<String> safeEffSet = agentLearnedSafeActionsEffects.get(actionName);

		return generateLearnedAction(safePreSet, safeEffSet, actionName, agentName);
	}

	private String generateUnSafeAction(String agentName, String actionName) {

		Set<String> unsafePreSet = agentLearnedUnSafeActionsPreconditions.get(actionName);
		Set<String> unsafeEffSet = agentLearnedUnSafeActionsEffects.get(actionName);

		return generateLearnedAction(unsafePreSet, unsafeEffSet, actionName, agentName);
	}


	private boolean writeLearnedActionsTMPfile(File safeActionsFile, File unsafeActionsFile) {

		Iterator<Entry<String, List<String>>> it = agentLearnedActionNames.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, List<String>> agentActionNames = (Entry<String, List<String>>)it.next();

			List<String> actionNames = agentActionNames.getValue();
			String agentName = agentActionNames.getKey();

			for (String actionName : actionNames) {			
				if(!writeSafeAction(safeActionsFile, agentName, actionName)) {
					LOGGER.info("Writing safe action " + actionName + " to tmp file failure!");
					return false;
				}

				if(!writeUnSafeAction(unsafeActionsFile, agentName, actionName)) {
					LOGGER.info("Writing unsafe action " + actionName + " to tmp file failure!");
					return false;
				}
			}
		}

		return true;
	}

	private boolean writeSafeAction(File safeActionsFile, String agentName, String actionName) {

		Set<String> safePreSet = agentLearnedSafeActionsPreconditions.get(actionName);
		Set<String> safeEffSet = agentLearnedSafeActionsEffects.get(actionName);

		String learnedSafeAction = generateLearnedAction(safePreSet, safeEffSet, actionName, agentName);

		Writer writer;
		try {
			writer = new BufferedWriter(new FileWriter(safeActionsFile, true));
			writer.write(learnedSafeAction);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private boolean writeUnSafeAction(File unsafeActionsFile, String agentName, String actionName) {

		Set<String> unsafePreSet = agentLearnedUnSafeActionsPreconditions.get(actionName);
		Set<String> unsafeEffSet = agentLearnedUnSafeActionsEffects.get(actionName);

		String learnedUnSafeAction = generateLearnedAction(unsafePreSet, unsafeEffSet, actionName, agentName);

		Writer writer;
		try {
			writer = new BufferedWriter(new FileWriter(unsafeActionsFile, true));
			writer.write(learnedUnSafeAction);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private boolean writeNewLearnedProblemFiles() {

		LOGGER.info("writing newly learned problem .pddl files");

		boolean anyLearned = false;

		for (String agentName : agentList) {

			List<String> learnedSafeActions = new ArrayList<String>();
			List<String> learnedUnSafeActions = new ArrayList<String>();

			for (String otherAgentName : agentList) {
				if(!otherAgentName.equals(agentName)) {
					learnedSafeActions.addAll(agentLearnedSafeActions.get(otherAgentName));
					learnedUnSafeActions.addAll(agentLearnedUnSafeActions.get(otherAgentName));
				}
			}

			if(!learnedSafeActions.isEmpty()) {

				anyLearned = true;

				writeActionsToFile(agentName, learnedSafeActions, true);
			}

			if(!learnedUnSafeActions.isEmpty()) {

				anyLearned = anyLearned && true;

				writeActionsToFile(agentName, learnedUnSafeActions, false);
			}
		}

		return anyLearned;
	}

	private boolean writeActionsToFile(String agentName, List<String> learnedActions, boolean safeModel) {

		String domainStr = readDomainString(agentName, safeModel);

		if(domainStr.isEmpty())
		{
			LOGGER.info("Reading domain pddl failure");
			return false;
		}

		String newDomainStr = addActionsToDomainString(learnedActions, domainStr ,agentName);

		if(newDomainStr.isEmpty())
		{
			LOGGER.info("Adding new actions to domain failure");
			return false;
		}

		if(!writeNewDomain(newDomainStr, agentName, safeModel)) {
			LOGGER.info("Writing new domain to file failure");
			return false;
		}

		return true;
	}

	private boolean writeNewDomain(String newDomainString, String agentName, boolean safeModel) {

		LOGGER.info("Writing new PDDL domain file");

		String learnedDomainPath = "";

		if (safeModel)
			learnedDomainPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
		else
			learnedDomainPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		try {
			FileUtils.writeStringToFile(new File(learnedDomainPath), newDomainString, Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return false;
		}

		return true;
	}

	private boolean writeNewSafeDomain(String newDomainString, String agentName) {

		LOGGER.info("Writing new safe PDDL domain file");

		String learnedDomainPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		try {
			FileUtils.writeStringToFile(new File(learnedDomainPath), newDomainString, Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return false;
		}

		return true;
	}

	private boolean writeNewUnSafeDomain(String newDomainString, String agentName) {

		LOGGER.info("Writing new unsafe PDDL domain file");

		String learnedDomainPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		try {
			FileUtils.writeStringToFile(new File(learnedDomainPath), newDomainString, Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return false;
		}

		return true;
	}



	private String addActionsToDomainString(List<String> learnedActions, String domainString , String agentName) {

		LOGGER.info("Adding new actions to domain string");

		StringBuilder sb = new StringBuilder(domainString);
		int end = sb.lastIndexOf(")");

		if(end == -1)
			return "";
		else {
			for (String action : learnedActions) {
				sb.insert(end, action);
				end += action.length();
			}
		}

		return sb.toString();
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

	private String readDomainString(String agentName, boolean safeModel) {

		LOGGER.info("Reading domain pddl");

		String learnedDomainPath = "";

		if (safeModel)
			learnedDomainPath = Globals.OUTPUT_SAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;
		else
			learnedDomainPath = Globals.OUTPUT_UNSAFE_MODEL_PATH + "/" + agentName + "/" + domainFileName;

		String fileStr = "";

		try {
			fileStr = FileUtils.readFileToString(new File(learnedDomainPath),Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return "";
		}

		return fileStr;
	}

	private String readDomainString(String agentName) {

		LOGGER.info("Reading domain pddl");

		String learnedDomainPath = localViewFiles + "/" + agentName + "/" + domainFileName;

		String fileStr = "";

		try {
			fileStr = FileUtils.readFileToString(new File(learnedDomainPath),Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return "";
		}

		return fileStr;
	}

	/*
	private String generateLearnedAction(Set<String> pre, Set<String> eff, Action action) {

		LOGGER.info("Building action string");

		String rep = "";

		String actionName = action.getSimpleLabel().split(" ")[0];

		rep += "(:action " + actionName + "\n";
		rep += "\t:agent ?" + action.getOwner() + " - " + action.getOwner() + "\n";
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
	 */


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


	/*
	private Set<String> learnPreconditions(List<StateActionState> sasList) {

		LOGGER.info("Learning preconditions");

		Set<String> res = new HashSet<String>();

		State preState = sasList.get(0).pre;

		res.addAll(getStateFacts(preState));

		for (StateActionState sas : sasList) {
			res.retainAll(getStateFacts(sas.pre));
		}

		return res;
	}

	private Set<String> learnEffects(List<StateActionState> sasList) {

		LOGGER.info("Learning effects");

		Set<String> res = new HashSet<String>();

		for (StateActionState sas : sasList) {

			State preState = sas.pre;
			State postState = sas.post;

			List<String> preFacts = getStateFacts(preState);
			List<String> postFacts = getStateFacts(postState);

			postFacts.removeAll(preFacts);

			res.addAll(postFacts);		
		}

		return res;
	}
	 */


	private Set<String> learnSafePreconditions(List<StateActionState> sasList) {

		LOGGER.info("Learning preconditions");

		Set<String> res = new HashSet<String>();

		res.addAll(sasList.get(0).pre);

		for (StateActionState sas : sasList) {
			res.retainAll(sas.pre);
		}

		return res;
	}

	private Set<String> learnUnSafePreconditions(List<StateActionState> sasList) {

		LOGGER.info("Learning preconditions");

		Set<String> res = new HashSet<String>();

		return res;
	}

	private Set<String> learnSafeEffects(List<StateActionState> sasList) {

		LOGGER.info("Learning effects");

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

		LOGGER.info("Learning effects");

		Set<String> res = new HashSet<String>();

		res.addAll(sasList.get(0).post);

		for (StateActionState sas : sasList) {
			res.retainAll(sas.post);
		}

		return res;
	}


	private List<String> getStateFacts(State state) {

		LOGGER.info("Extracting facts from state " + state);

		List<String> res = new ArrayList<String>();

		String str = new String(state.getDomain().humanize(state.getValues()));

		int start = str.indexOf('=');

		while(start != -1) {

			int end = str.indexOf(')');

			String var = str.substring(start + 1,end + 1);

			var = var.replace("(", " ");
			var = var.replace(",", "");
			var = var.replace(")", "");

			res.add(var);

			str = str.substring(end + 1, str.length());		

			start = str.indexOf('=');
		}

		return res;
	}

	/*
	private List<StateActionState> getAllStateActionStateWithAction(List<StateActionState> trajectorySequences, Action action) {

		LOGGER.info("Getting all StateActionState's with action " + getActionName(action));

		List<StateActionState> res = new ArrayList<StateActionState>();

		for (StateActionState sas: trajectorySequences) {
			if(getActionName(sas.action).equals(getActionName(action)))
				res.add(sas);
		}

		return res;
	}
	 */


	private List<StateActionState> getAllStateActionStateWithAction(List<StateActionState> trajectorySequences, String action) {

		LOGGER.info("Getting all StateActionState's with action " + action);

		List<StateActionState> res = new ArrayList<StateActionState>();

		for (StateActionState sas: trajectorySequences) {
			if(sas.action.equals(action))
				res.add(sas);
		}

		return res;
	}


	private String getActionName(Action action) {

		LOGGER.info("Getting action for: " + action.getSimpleLabel());

		Field field = null;

		try {
			field = Action.class.getDeclaredField("name");

			if(field != null) {
				field.setAccessible(true);
				Object value = field.get(action);
				return value.toString();
			}
		} catch (Exception e) {
			LOGGER.info(e,e);
			return "";
		}

		return "";
	}
}