package PlannerAndLearner;

import java.util.LinkedHashSet;
import java.util.Set;

public class Action {

	Set<String> preconditions;
	Set<String> effects;

	String agent;

	String agentLine;
	String parametersLine;

	String name;
	
	boolean isParam;

	public Action() {
		preconditions = new LinkedHashSet<String>();
		effects = new LinkedHashSet<String>();

		agent = "";
		agentLine = "";
		parametersLine = "";
		name = "";
		isParam = false;
	}
	
	public Action(Action toCopy) {
		preconditions = new LinkedHashSet<String>(toCopy.preconditions);
		effects = new LinkedHashSet<String>(toCopy.effects);

		agent = toCopy.agent;
		agentLine = toCopy.agentLine;
		parametersLine = toCopy.parametersLine;
		name = toCopy.name;
		isParam = toCopy.isParam;
	}
	
	@Override
	public String toString() {
		return name;
	}

}
