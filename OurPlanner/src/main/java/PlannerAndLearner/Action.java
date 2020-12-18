package PlannerAndLearner;

import java.util.LinkedHashSet;
import java.util.Set;

public class Action {

	public 	Set<String> preconditions;
	public 	Set<String> effects;

	public 	String agent;

	public String agentLine;
	public 	String parametersLine;

	public 	String name;
	
	public boolean isParam;

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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agent == null) ? 0 : agent.hashCode());
		result = prime * result + ((agentLine == null) ? 0 : agentLine.hashCode());
		result = prime * result + ((effects == null) ? 0 : effects.hashCode());
		result = prime * result + (isParam ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parametersLine == null) ? 0 : parametersLine.hashCode());
		result = prime * result + ((preconditions == null) ? 0 : preconditions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Action other = (Action) obj;
		if (agent == null) {
			if (other.agent != null)
				return false;
		} else if (!agent.equals(other.agent))
			return false;
		if (agentLine == null) {
			if (other.agentLine != null)
				return false;
		} else if (!agentLine.equals(other.agentLine))
			return false;
		if (effects == null) {
			if (other.effects != null)
				return false;
		} else if (!effects.equals(other.effects))
			return false;
		if (isParam != other.isParam)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parametersLine == null) {
			if (other.parametersLine != null)
				return false;
		} else if (!parametersLine.equals(other.parametersLine))
			return false;
		if (preconditions == null) {
			if (other.preconditions != null)
				return false;
		} else if (!preconditions.equals(other.preconditions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

}
