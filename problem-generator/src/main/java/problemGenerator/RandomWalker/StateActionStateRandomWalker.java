package problemGenerator.RandomWalker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import Model.SASPreprocessor;
import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.Domain;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;
import cz.agents.dimaptools.model.SuperState;
import problemGenerator.StateActionState;

public class StateActionStateRandomWalker {

	private final static Logger LOGGER = Logger.getLogger(StateActionStateRandomWalker.class);

	public static State RandomWalk(List<StateActionState> sasList, SASPreprocessor preprocessor,
			int numOfExpands, List<Problem> problems, int trace_number) {

		// Random rand = new Random(1); // fixed seed
		Random rand = new Random(); // no seed

		LOGGER.info("start random walk:");

		int randomActionIndex;
		int randomAgent;
		Action randomAction;

		State startState = preprocessor.getGlobalInit();
		SuperState goalState = preprocessor.getGlobalGoal();

		State current = new State(startState);

		List<Action> applicableActions = new ArrayList<Action>();

		for (int i = 0; i < numOfExpands; i++) {

			randomAgent = rand.nextInt(problems.size());
			Problem problem = problems.get(randomAgent);

			applicableActions.clear();

			for (Action action : problem.getMyActions()) {
				if (action.isApplicableIn(current)) {
					applicableActions.add(action);
				}
			}

			// removes any action that transforms to the goal state
			if(goalState != null)
			{
				List<Action> goalReachingActions= new ArrayList<Action>();

				for (Action action : applicableActions) {

					State currentCopy = new State(current);

					action.transform(currentCopy);

					if (currentCopy.unifiesWith(goalState)) {
						goalReachingActions.add(action);
					}
				}

				applicableActions.removeAll(goalReachingActions);
			}

			// If reached a dead-end, stop the random walk
			if (applicableActions.isEmpty()) {
				LOGGER.info(problem.agent + " - no more applicable actions!");
				return current;
			}

			randomActionIndex = rand.nextInt(applicableActions.size());
			randomAction = applicableActions.get(randomActionIndex);

			State pre = new State(current);

			randomAction.transform(current);

			if(sasList!=null) {

				String actionName = randomAction.getSimpleLabel();

				Map<Integer, Set<Integer>> varDomains = problem.getDomain().getVariableDomains();

				StateActionState sas = new StateActionState(getStateFacts(pre, varDomains),
						actionName, randomAction.getOwner(), getStateFacts(current, varDomains), trace_number);

				sasList.add(sas);
			}

			// LOGGER.info("Reached state " + current);

			LOGGER.info("End of iteration");
		}

		LOGGER.info("end random walk:");

		return current;
	}

	private static Set<String> getStateFacts(State state, Map<Integer, Set<Integer>> varDomains) {

		LOGGER.info("Extracting facts from state " + state);

		Set<String> out = new HashSet<String>();

		int[] values = state.getValues();

		for(int var = 0; var < values.length; ++var){

			if(var >= 0)
				addFactToOut(varDomains, out, values, var);
		}

		return out;
	}

	private static void addFactToOut(Map<Integer, Set<Integer>> varDomains, Set<String> out, int[] values, int var) {
		
		String newVal = Domain.valNames.get(values[var]).toString();

		if(newVal.startsWith("NONE")) {
			
			for (int val : varDomains.get(var)) {
				newVal = Domain.valNames.get(val).toString();

				if(val!=values[var])
					out.add("not ("+ newVal + ")");
			}

		}
		else {
			out.add(newVal);
		}
	}

	public static State RandomWalk(List<StateActionState> sasList, State startState, SuperState goalState, 
			int numOfExpands, List<Problem> problems, int trace_number) {

		// Random rand = new Random(1); // fixed seed
		Random rand = new Random(); // no seed

		LOGGER.info("start random walk:");

		int randomActionIndex;
		int randomAgent;
		Action randomAction;
		State current = new State(startState);

		List<Action> applicableActions = new ArrayList<Action>();

		for (int i = 0; i < numOfExpands; i++) {

			randomAgent = rand.nextInt(problems.size());
			Problem problem = problems.get(randomAgent);

			applicableActions.clear();

			for (Action action : problem.getMyActions()) {
				if (action.isApplicableIn(current)) {
					applicableActions.add(action);
				}
			}

			// removes any action that transforms to the goal state
			if(goalState != null)
			{
				List<Action> goalReachingActions= new ArrayList<Action>();

				for (Action action : applicableActions) {

					State currentCopy = new State(current);

					action.transform(currentCopy);

					if (currentCopy.unifiesWith(goalState)) {
						goalReachingActions.add(action);
					}
				}

				applicableActions.removeAll(goalReachingActions);
			}

			// If reached a dead-end, stop the random walk
			if (applicableActions.isEmpty()) {
				LOGGER.info(problem.agent + " - no more applicable actions!");
				return current;
			}

			randomActionIndex = rand.nextInt(applicableActions.size());
			randomAction = applicableActions.get(randomActionIndex);

			State pre = new State(current);

			randomAction.transform(current);

			if(sasList!=null) {

				String actionName = randomAction.getSimpleLabel();

				StateActionState sas = new StateActionState(getStateFacts(pre), actionName, randomAction.getOwner(),
						getStateFacts(current), trace_number);

				sasList.add(sas);
			}

			// LOGGER.info("Reached state " + current);

			LOGGER.info("End of iteration");
		}

		LOGGER.info("end random walk:");

		return current;
	}

	private static Set<String> getStateFacts(State state) {

		LOGGER.info("Extracting facts from state " + state);

		Set<String> out = new HashSet<String>();

		int[] values = state.getValues();

		for(int var = 0; var < values.length; ++var){
			if(var >= 0){

				String newVal = Domain.valNames.get(values[var]).toString();

				out.add(newVal);
			}
		}

		return out;
	}
}
