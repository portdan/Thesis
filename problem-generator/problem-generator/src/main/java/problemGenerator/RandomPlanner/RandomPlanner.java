package problemGenerator.RandomPlanner;

import org.apache.log4j.Logger;

import cz.agents.alite.communication.Communicator;
import cz.agents.alite.configurator.ConfigurationInterface;
import cz.agents.dimaptools.DIMAPWorldInterface;
import cz.agents.dimaptools.model.Problem;
import cz.agents.dimaptools.search.SearchInterface;
import cz.agents.madla.executor.PlanExecutorInterface;

public class RandomPlanner {
	
	private final static Logger LOGGER = Logger.getLogger(RandomPlanner.class);
	
	private final DIMAPWorldInterface world;
	private final Problem problem;
	private final Communicator comm;

	private final PlanExecutorInterface executor;

	private SearchInterface search;
	private ConfigurationInterface plannerConfig;
	
	
	private long timeLimitMs = Long.MAX_VALUE;


	public RandomPlanner(DIMAPWorldInterface world, Problem problem, Communicator comm, PlanExecutorInterface executor,
			SearchInterface search, ConfigurationInterface plannerConfig, long timeLimitMs) {
		super();
		this.world = world;
		this.problem = problem;
		this.comm = comm;
		this.executor = executor;
		this.search = search;
		this.plannerConfig = plannerConfig;
		this.timeLimitMs = timeLimitMs;
	}
	
	
	

}
