package OurPlanner;

import java.util.List;

public class PlanToStateActionStateResult {

	List<StateActionState> planSASList;

	StateActionState failedActionSAS;

	public PlanToStateActionStateResult(List<StateActionState> planSASList, StateActionState failedActionSAS) {
		super();
		this.planSASList = planSASList;
		this.failedActionSAS = failedActionSAS;
	}

	public List<StateActionState> getPlanSASList() {
		return planSASList;
	}

	public StateActionState getFailedActionSAS() {
		return failedActionSAS;
	}
}