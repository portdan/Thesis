package problemGenerator.RandomPlanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import cz.agents.alite.communication.Communicator;
import cz.agents.alite.communication.Message;
import cz.agents.dimaptools.DIMAPWorldInterface;
import cz.agents.dimaptools.communication.message.StateMessage;
import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.model.State;

public class RandomPlanner {

	private final static Logger LOGGER = Logger.getLogger(RandomPlanner.class);

	private final DIMAPWorldInterface world;
	private final Problem problem;

	private int numOfExpands;

	private long timeLimitMs = Long.MAX_VALUE;

	private Random rand;
	
	public State initialState,endState;

	public RandomPlanner(DIMAPWorldInterface world,  int numOfExpands, long timeLimitMs) {
		this.world = world;
		this.problem = world.getProblem();
		this.numOfExpands = numOfExpands;
		this.timeLimitMs = timeLimitMs;

		rand = new Random();
		
		initialState =  new State(problem.initState);
		endState=null;
	}	
	
	
	public static void RandomWalk(State current, State endState,  int numOfExpands, List<DIMAPWorldInterface> worlds) {
		Random rand = new Random();
		LOGGER.info("start random walk:");

		// Random walk TODO: MAKE IT WORK DANIEL!
		int randomActionIndex;
		int randomAgent;
		Action randomAction;
		Set<State> pastStates=  new HashSet();
		State copyOfNewState=new State(current);
		pastStates.add(copyOfNewState);
		
		
		List<Action> applicableActions = new ArrayList<Action>();
		List<Action> newApplicableActions = new ArrayList<Action>();

		
		for(int i=0; i<numOfExpands; i++) {
			randomAgent = rand.nextInt(worlds.size());
			DIMAPWorldInterface world = worlds.get(randomAgent);
			
			applicableActions.clear();
			newApplicableActions.clear();
			for(Action action : world.getProblem().getMyActions()) {
				if(action.isApplicableIn(current)) {
					applicableActions.add(action);
					copyOfNewState = new State(current);
					action.transform(copyOfNewState);
					if(pastStates.contains(copyOfNewState)==false) {
						newApplicableActions.add(action);
					}
				}
			}

			// PRIORITIZE GOING TO NEW STATES
			if(newApplicableActions.isEmpty()) {
				LOGGER.info(world.getAgentName() +  " - all applicable actions lead to already visited states");
			}
			else {
				applicableActions.clear();
				applicableActions.addAll(newApplicableActions);
			}
			
			// If reached a dead-end, stop the random walk
			if(applicableActions.isEmpty()) {		
				LOGGER.info(world.getAgentName() +  " - no more applicable actions!");
				endState = current;
				return;
			}
			
			randomActionIndex = rand.nextInt(applicableActions.size());
			randomAction = applicableActions.get(randomActionIndex);
			randomAction.transform(current);
			
			pastStates.add(copyOfNewState);

			LOGGER.info(world.getAgentName() +  " applied action " + randomAction.getLabel());
			LOGGER.info("Reached state " + current);
			LOGGER.info("End of iteration");
			/*
	        StateMessage msg = new StateMessage(current.getValues(),0, 0);
	        Communicator communicator = world.getCommunicator();
	        
			Message message = communicator.createMessage(world.getEncoder().encodeStateMessage(msg));
	        communicator.sendMessage(message);			
			//current = randomAction.get
			 */
		}

		LOGGER.info("end random walk:");

		// TODO: PRINT OUT THE NEW STATE, AND CREATE FROM IT A PDDL

		endState = current;
	}

	public static State RandomWalk(State startState, int numOfExpands, List<DIMAPWorldInterface> worlds) {
		
		//Random rand = new Random(1); // fixed seed
		Random rand = new Random(); // no seed

		LOGGER.info("start random walk:");

		// Random walk TODO: MAKE IT WORK DANIEL!
		int randomActionIndex;
		int randomAgent;
		Action randomAction;
		State current = new State(startState);
		
		List<Action> applicableActions = new ArrayList<Action>();
		
		for(int i=0; i<numOfExpands; i++) {
			
			randomAgent = rand.nextInt(worlds.size());
			DIMAPWorldInterface world = worlds.get(randomAgent);
			
			applicableActions.clear();

			for(Action action : world.getProblem().getMyActions()) {
				if(action.isApplicableIn(current)) {
					applicableActions.add(action);	
				}
			}
			
			// If reached a dead-end, stop the random walk
			if(applicableActions.isEmpty()) {		
				LOGGER.info(world.getAgentName() +  " - no more applicable actions!");
				return current;
			}
			
			randomActionIndex = rand.nextInt(applicableActions.size());
			randomAction = applicableActions.get(randomActionIndex);
			
			
			
			LOGGER.info(world.getAgentName() +  " applied action " + randomAction.getLabel());
			LOGGER.info("Previous state " + current);
			
			randomAction.transform(current);
			
			LOGGER.info("Reached state " + current);
			
			LOGGER.info("End of iteration");
		}

		LOGGER.info("end random walk:");
		
		return current;

		// TODO: PRINT OUT THE NEW STATE, AND CREATE FROM IT A PDDL
	}

	public void RandomWalk () {

		LOGGER.info("start random walk:");

		// Random walk TODO: MAKE IT WORK DANIEL!
		int randomActionIndex;

		Action randomAction;

		State current = problem.initState;

		List<Action> applicableActions = new ArrayList<Action>();

		for(int i=0; i<numOfExpands; i++) {

			getApplicableActions(applicableActions,current);

			// IF NO APPLICABLE ACTION - STOP!
			if(applicableActions.isEmpty()) {		
				LOGGER.info(world.getAgentName() +  " - no more applicable actions!");
				endState = current;
			}
			randomActionIndex = rand.nextInt(applicableActions.size());
			randomAction = applicableActions.get(randomActionIndex);
			randomAction.transform(current);

			/*
	        StateMessage msg = new StateMessage(current.getValues(),0, 0);
	        Communicator communicator = world.getCommunicator();
	        
			Message message = communicator.createMessage(world.getEncoder().encodeStateMessage(msg));
	        communicator.sendMessage(message);			
			//current = randomAction.get
			 */
		}

		LOGGER.info("end random walk:");

		// TODO: PRINT OUT THE NEW STATE, AND CREATE FROM IT A PDDL

		endState = current;
	}

	private void getApplicableActions(List<Action> applicableActions, State current) {

		applicableActions.clear();	

		for(Action action : problem.getMyActions()) {
			if(action.isApplicableIn(current))
				applicableActions.add(action);
		}
	}

	
	public String getName() {
		return world.getAgentName();
	}

}
