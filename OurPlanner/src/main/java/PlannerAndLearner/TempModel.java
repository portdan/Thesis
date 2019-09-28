package PlannerAndLearner;

import java.util.LinkedHashSet;
import java.util.Set;

public class TempModel {

	public Set<TempAction> tempActions;

	public TempModel() {
		tempActions = new LinkedHashSet<TempAction>();
	}

	public TempModel(TempModel toCopy) {
		tempActions = new LinkedHashSet<TempAction>();

		for (TempAction tempAction : toCopy.tempActions)
			tempActions.add(new TempAction(tempAction));
	}

	public TempAction getTempActionByName(String actionName) {

		for (TempAction tempAction : tempActions) {
			if(tempAction.name.equals(actionName))
				return tempAction;
		}

		return null;
	}

	public TempAction popTempActionByName(String actionName) {

		TempAction res = null;
		TempAction toRemove = null;

		for (TempAction tempAction : tempActions) {
			if(tempAction.name.equals(actionName)) {
				res = new TempAction(tempAction);
				toRemove = tempAction;
			}
		}			

		tempActions.remove(toRemove);
		
		return res;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((tempActions == null) ? 0 : tempActions.hashCode());
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
		TempModel other = (TempModel) obj;
		if (tempActions == null) {
			if (other.tempActions != null)
				return false;
		} else if (!tempActions.equals(other.tempActions))
			return false;
		return true;
	}

	@Override
	public String toString() {

		String str = "{ ";

		for (TempAction tempAction : tempActions) {
			str += tempAction.toString() + '\n';
		}

		str = str.substring(0, str.length() - 1);

		str += " }";

		return str;
	}
}
