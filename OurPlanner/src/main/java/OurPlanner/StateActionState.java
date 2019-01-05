package OurPlanner;

import cz.agents.dimaptools.model.Action;
import cz.agents.dimaptools.model.State;

public class StateActionState {

	State pre = null;
	Action action = null;
	State post = null;

	public StateActionState(State pre, Action action, State post) {

		this.pre = new State(pre);
		this.action = new Action(action);
		this.post = new State(post);
	}

}
