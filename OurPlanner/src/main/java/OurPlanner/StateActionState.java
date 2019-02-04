package OurPlanner;

import java.util.HashSet;
import java.util.Set;

public class StateActionState {

	/*
	State pre = null;
	Action action = null;
	State post = null;

	public StateActionState(State pre, Action action, State post) {

		this.pre = new State(pre);
		this.action = new Action(action);
		this.post = new State(post);
	}
	 */


	Set<String> pre = null;
	String action = null;
	String actionOwner = null;
	Set<String> post = null;

	public StateActionState(Set<String> pre, String action, String actionOwner, Set<String> post) {

		this.pre = new HashSet<String>(pre);
		this.action = new String(action);
		this.actionOwner = new String(actionOwner);
		this.post = new HashSet<String>(post);
	}


}
