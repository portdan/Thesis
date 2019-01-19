package OurPlanner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.State;

public class TrajectoryLearner {

	private final static Logger LOGGER = Logger.getLogger(TrajectoryLearner.class);

	private String learningAgentName = "";
	private List<String> agentList = null;
	private File groundedFile = null;
	private File trajectoriesFile = null;
	private File localViewFile = null;
	private String domainFileName = "";
	private String problemFileName = "";

	List<StateActionState> trajectorySequences = new ArrayList<StateActionState>();
	private Random randomGenerator = new Random();

	public TrajectoryLearner(String learningAgentName ,List<String> agentList, File groundedFile, File trajectoriesFile, 
			File localViewFile, String domainFileName, String problemFileName) {

		LOGGER.info("TrajectoryLearner constructor");

		this.learningAgentName = learningAgentName;
		this.agentList = agentList;
		this.groundedFile = groundedFile;
		this.trajectoriesFile = trajectoriesFile;
		this.localViewFile = localViewFile;
		this.domainFileName = new String(domainFileName);
		this.problemFileName = new String(problemFileName);

		logInput();

		generateSequences();
	}

	private void generateSequences() {

		LOGGER.info("Generating sequences");

		StateActionStateSequencer sequencer = 
				new StateActionStateSequencer(agentList, groundedFile.getPath(),
						domainFileName,problemFileName);

		File[] TrajDir = trajectoriesFile.listFiles();
		File[] ProbDir = groundedFile.listFiles();

		if (TrajDir != null) 
			for (File trj : TrajDir) {

				String trajectoryPath = trj.getPath();
				String trajectoryName = FilenameUtils.getBaseName(trajectoryPath);

				String ext = FilenameUtils.getExtension(trajectoryPath); 

				if(ext.equals(Globals.TRAJECTORY_FILE_EXTENSION)) {

					LOGGER.info("Generating sequence for trajectory in " + trajectoryName);

					if (ProbDir != null) 
						for (File prb : ProbDir) {

							String problemPath = prb.getPath();
							String problemName = FilenameUtils.getBaseName(problemPath);

							if(problemName.equals(trajectoryName)) {		

								List<StateActionState> res = sequencer.generateSequance(learningAgentName, trajectoryPath);
								trajectorySequences.addAll(res);
							}
						}
				}

			}
	}

	private void logInput() {

		LOGGER.info("Logging input");

		LOGGER.info("learningAgentName: " + learningAgentName);
		LOGGER.info("agentList: " + agentList);
		LOGGER.info("groundedFile: " + groundedFile);
		LOGGER.info("trajectoriesFile: " + trajectoriesFile);
		LOGGER.info("localViewFile: " + localViewFile);
		LOGGER.info("domainFileName: " + domainFileName);
		LOGGER.info("problemFileName: " + problemFileName);
	}

	public boolean learnNewActions() {

		LOGGER.info("Learning new actions");

		List<String> learnedActions = new ArrayList<String>();

		while(!trajectorySequences.isEmpty()) {

			StateActionState firstValue = getRandomTrajectory();

			LOGGER.info("Learning action: " + getActionName(firstValue.action));

			List<StateActionState> sasList = getAllStateActionStateWithAction(firstValue.action);

			if(firstValue.action.getOwner().equals(learningAgentName)) {

				Set<String> pre = learnPreconditions(sasList);
				Set<String> eff = learnEffects(sasList);

				learnedActions.add(generateLearnedActionString(pre,eff,firstValue.action));
			}

			trajectorySequences.removeAll(sasList);

		}

		return writeNewLearnedProblemFiles(learnedActions);
	}

	public StateActionState getRandomTrajectory(){

		LOGGER.info("Get a random value from trajectorySequences list");
		int index = randomGenerator.nextInt(trajectorySequences.size());
		StateActionState item = trajectorySequences.get(index);
		return item;
	}

	private boolean writeNewLearnedProblemFiles(List<String> learnedActions) {

		LOGGER.info("writing newly learned problem .pddl files");

		String domainStr = readDomainString();

		if(domainStr.isEmpty())
		{
			LOGGER.info("Reading domain pddl failure");
			return false;
		}

		domainStr = addActionsToDomainString(learnedActions,domainStr ,learningAgentName);

		if(domainStr.isEmpty())
		{
			LOGGER.info("Adding new actions to domain failure");
			return false;
		}

		if(!writeNewDomain(domainStr)) {
			LOGGER.info("Writing new domain to file failure");
			return false;
		}

		return true;
	}

	private boolean writeNewDomain(String newDomainString) {

		LOGGER.info("Writing new PDDL domain file");

		String learnedDomainPath = Globals.LEARNED_PATH + "/" + learningAgentName + "/" + domainFileName;

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

		String learnedDomainPath = Globals.LEARNED_PATH + "/" + learningAgentName + "/" + domainFileName;

		String fileStr = "";

		try {
			fileStr = FileUtils.readFileToString(new File(learnedDomainPath),Charset.defaultCharset());
		} catch (IOException e) {
			LOGGER.info(e,e);
			return "";
		}

		return fileStr;
	}

	private String generateLearnedActionString(Set<String> pre, Set<String> eff, Action action) {

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
		rep += ")\n\n";

		return rep;
	}

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

	private List<StateActionState> getAllStateActionStateWithAction(Action action) {

		LOGGER.info("Getting all StateActionState's with action " + getActionName(action));

		List<StateActionState> res = new ArrayList<StateActionState>();

		for (StateActionState sas: trajectorySequences) {
			if(getActionName(sas.action).equals(getActionName(action)))
				res.add(sas);
		}

		return res;
	}

	private String getActionName(Action action) {

		LOGGER.info("Getting action for: " + action.getSimpleLabel());

		/*
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
		 */

		return action.getSimpleLabel(); 
	}
}