package PlannerAndLearner;

import java.util.Map;

public class LikelyModelStatus {

	Map<String, Boolean> agentActionsPresesnt;

	public Map<String, Boolean> getAgentActionsPresesnt() {
		return agentActionsPresesnt;
	}

	public void setAgentActionsPresesnt(Map<String, Boolean> agentActionsPresesnt) {
		this.agentActionsPresesnt = agentActionsPresesnt;
	}

	public LikelyModelStatus(Map<String, Boolean> agentActionsPresesnt) {
		super();
		this.agentActionsPresesnt = agentActionsPresesnt;
	}

	
}