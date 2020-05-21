package PlannerAndLearner;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import OurPlanner.StateActionState;

public class ModelSearchNode {

	private ModelSearchNode parent;
	private List<ModelSearchNode> children;

	private TempModel tempModel;
	private List<StateActionState> planSASList;
	private int goalProximity;

	private int numOfVisits;
	private double value;

	public int getNumOfVisits() {
		return numOfVisits;
	}

	public void setNumOfVisits(int numOfVisits) {
		this.numOfVisits = numOfVisits;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public List<ModelSearchNode> getChildren() {
		return children;
	}

	public void setChildren(List<ModelSearchNode> children) {
		this.children = children;
	}

	public ModelSearchNode getParent() {
		return parent;
	}

	public void setParent(ModelSearchNode parent) {
		this.parent = parent;
	}

	public TempModel getTempModel() {
		return tempModel;
	}

	public void setTempModel(TempModel tempModel) {
		this.tempModel = tempModel;
	}

	public List<StateActionState> getPlanSASList() {
		return planSASList;
	}

	public void setPlanSASList(List<StateActionState> planSASList) {
		this.planSASList = planSASList;
	}

	public ModelSearchNode(ModelSearchNode parent, TempModel tempModel) {
		this.parent = parent;
		this.children = null;
		this.tempModel = tempModel;
		this.goalProximity = 0;
		//this.numOfVisits = 0;
		this.numOfVisits = 1;
		this.value = 0;
	}

	public int getReliabilityHeuristic(Map<String, Map<String, Integer>> actionsPreconditionsScore) {
		int reliability = 0;

		for (TempAction tempAct : tempModel.tempActions) {

			Map<String, Integer> actionPreconditionsScore = actionsPreconditionsScore.get(tempAct.name);

			if(actionPreconditionsScore != null)
				for (String pre : tempAct.preconditionsAdd)
					reliability += actionPreconditionsScore.get(pre);
		}

		return reliability;
	}

	public int getPlanLengthHeuristic() {

		if (parent == null) 
			return 0;
		else if(parent.getPlanSASList() == null)
			return 0;
		else
			return parent.getPlanSASList().size();
	}

	public int getGoalProximityHeuristic() {
		return goalProximity;
	}

	public void calcGoalProximity(Set<String> goalFacts) {
		if(planSASList == null)
			goalProximity = 0;
		else if(planSASList.size() == 0)
			goalProximity = 0;
		else {
			Set<String> lastValidStateEff = new HashSet<String> (planSASList.get(planSASList.size()-1).post);

			lastValidStateEff.retainAll(goalFacts);

			goalProximity = lastValidStateEff.size();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		//result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		//result = prime * result + ((planSASList == null) ? 0 : planSASList.hashCode());
		result = prime * result + ((tempModel == null) ? 0 : tempModel.hashCode());
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
		ModelSearchNode other = (ModelSearchNode) obj;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (planSASList == null) {
			if (other.planSASList != null)
				return false;
		} else if (!planSASList.equals(other.planSASList))
			return false;
		if (tempModel == null) {
			if (other.tempModel != null)
				return false;
		} else if (!tempModel.equals(other.tempModel))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "{ " + tempModel.toString() + " Value: " + value + " Visited: " + numOfVisits + " }";
	}
}
