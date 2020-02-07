package OurPlanner;

import java.util.List;

public class PlanToStateActionStateResult {

	List<StateActionState> planSASList;
	StateActionState failedActionSAS;
	boolean isTimeout;

	public PlanToStateActionStateResult(List<StateActionState> planSASList, 
			StateActionState failedActionSAS, boolean isTimeout) {
		super();
		this.planSASList = planSASList;
		this.failedActionSAS = failedActionSAS;
		this.isTimeout = isTimeout;
	}

	public List<StateActionState> getPlanSASList() {
		return planSASList;
	}

	public StateActionState getFailedActionSAS() {
		return failedActionSAS;
	}
	
	public boolean isTimeout() {
		return isTimeout;
	}
}