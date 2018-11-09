package StateActionStateSequencer;

import org.apache.log4j.Logger;

import cz.agents.alite.creator.Creator;

public class StateActionStateSequencer implements Creator {
	
	private final static Logger LOGGER = Logger.getLogger(StateActionStateSequencer.class);


	@Override
	public void create() {
		
		LOGGER.info("StateActionStateSequencer End");

	}
	@Override
	public void init(String[] args) {
		
		LOGGER.info("StateActionStateSequencer Start");

	}

}
