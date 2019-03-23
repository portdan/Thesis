package OurPlanner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.State;

public class TrajectoryLearner {

	private final static Logger LOGGER = Logger.getLogger(TrajectoryLearner.class);

	private String agentName = "";
	private List<String> agentList = null;

	private File trajectoryFiles = null;
	private File problemFiles = null;
	private File localViewFiles = null;

	private String domainFileName = "";
	private String problemFileName = "";

	private int numOfTracesToUse = 0;

	public TrajectoryLearner(List<String> agentList, String agentName ,File trajectoryFiles ,
			File problemFiles , File localViewFiles ,String domainFileName ,String problemFileName, int numOfTracesToUse) {

		LOGGER.info("TrajectoryLearner constructor");

		this.agentName = new String(agentName);
		this.agentList = agentList;
		this.trajectoryFiles = trajectoryFiles;
		this.problemFiles = problemFiles;
		this.localViewFiles = localViewFiles;
		this.domainFileName = new String(domainFileName);
		this.problemFileName = new String(problemFileName);
		this.numOfTracesToUse = numOfTracesToUse;

		logInput();
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("agentName: " + agentName);
		LOGGER.info("agentList: " + agentList);
		LOGGER.info("trajectoryFiles: " + trajectoryFiles);
		LOGGER.info("problemFiles: " + problemFiles);
		LOGGER.info("localViewFiles: " + localViewFiles);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
		LOGGER.info("numOfTracesToUse: " + numOfTracesToUse);
	}

	/*
	 * 	public boolean learnNewActions() {

		LOGGER.info("Learning new actions");

		List<String> learnedActions = new ArrayList<String>();

		StateActionStateSequencer sasSequencer = new StateActionStateSequencer(agentList, 
				problemFiles, domainFileName, problemFileName, trajectoryFiles);

		List<StateActionState> trajectorySequences = sasSequencer.generateSequences();

		while(!trajectorySequences.isEmpty()) {

			StateActionState firstValue = (StateActionState)trajectorySequences.get(0);

			//LOGGER.info("Learning action: " + getActionName(firstValue.action));

			LOGGER.info("Learning action: " + firstValue.action);

			List<StateActionState> sasList = getAllStateActionStateWithAction(trajectorySequences, firstValue.action);

			if(!firstValue.action.getOwner().equals(agentName)) {

				Set<String> pre = learnPreconditions(sasList);
				Set<String> eff = learnEffects(sasList);

				learnedActions.add(generateLearnedAction(pre,eff,firstValue.action));
			}

			trajectorySequences.removeAll(sasList);
		}

		return writeNewLearnedProblemFiles(learnedActions);
	}
	 */

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

	private boolean writeNewDomain(String newDomainString) {

		LOGGER.info("Writing new PDDL domain file");

		String learnedDomainPath = Globals.LEARNED_PATH + "/" + agentName + "/" + domainFileName;

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

	private String readDomainString() {

		LOGGER.info("Reading domain pddl");

		String learnedDomainPath = Globals.LEARNED_PATH + "/" + agentName + "/" + domainFileName;

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


	private Set<String> learnPreconditions(List<StateActionState> sasList) {

		LOGGER.info("Learning preconditions");

		Set<String> res = new HashSet<String>();

		res.addAll(sasList.get(0).pre);

		for (StateActionState sas : sasList) {
			res.retainAll(sas.pre);
		}

		return res;
	}

	private Set<String> learnEffects(List<StateActionState> sasList) {

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