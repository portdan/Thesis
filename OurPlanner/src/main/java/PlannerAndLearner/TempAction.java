package PlannerAndLearner;

import java.util.LinkedHashSet;
import java.util.Set;

public class TempAction {

	public Set<String> preconditionsAdd;
	public Set<String> preconditionsSub;
	public Set<String> effectsAdd;
	public Set<String> effectsSub;

	public String name;

	public TempAction() {
		preconditionsAdd = new LinkedHashSet<String>();
		preconditionsSub = new LinkedHashSet<String>();
		effectsAdd = new LinkedHashSet<String>();
		effectsSub = new LinkedHashSet<String>();

		name = "";
	}

	public TempAction(TempAction toCopy) {
		preconditionsAdd = new LinkedHashSet<String>(toCopy.preconditionsAdd);
		preconditionsSub = new LinkedHashSet<String>(toCopy.preconditionsSub);
		effectsAdd = new LinkedHashSet<String>(toCopy.effectsAdd);
		effectsSub = new LinkedHashSet<String>(toCopy.effectsSub);

		name = new String(toCopy.name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((effectsAdd == null) ? 0 : effectsAdd.hashCode());
		result = prime * result + ((effectsSub == null) ? 0 : effectsSub.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((preconditionsAdd == null) ? 0 : preconditionsAdd.hashCode());
		result = prime * result + ((preconditionsSub == null) ? 0 : preconditionsSub.hashCode());
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
		TempAction other = (TempAction) obj;
		if (effectsAdd == null) {
			if (other.effectsAdd != null)
				return false;
		} else if (!effectsAdd.equals(other.effectsAdd))
			return false;
		if (effectsSub == null) {
			if (other.effectsSub != null)
				return false;
		} else if (!effectsSub.equals(other.effectsSub))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (preconditionsAdd == null) {
			if (other.preconditionsAdd != null)
				return false;
		} else if (!preconditionsAdd.equals(other.preconditionsAdd))
			return false;
		if (preconditionsSub == null) {
			if (other.preconditionsSub != null)
				return false;
		} else if (!preconditionsSub.equals(other.preconditionsSub))
			return false;
		return true;
	}

	@Override
	public String toString() {

		String res = "";

		res += name;

		if(!preconditionsAdd.isEmpty()) {
			res += " [ preconditionsAdd: ";
			for (String str : preconditionsAdd) {
				res+= str +", ";
			}
			res += "] "; 
		}
		if(!preconditionsSub.isEmpty()) {
			res += " [ preconditionsSub: ";
			for (String str : preconditionsSub) {
				res+= str +", ";
			}
			res += "] "; 
		}
		if(!effectsAdd.isEmpty()) {
			res += " [ effectsAdd: ";
			for (String str : effectsAdd) {
				res+= str +", ";
			}
			res += "] "; 
		}
		if(!effectsSub.isEmpty()) {
			res += " [ effectsSub: ";
			for (String str : effectsSub) {
				res+= str +", ";
			}
			res += "] "; 
		}

		return res;
	}

}
