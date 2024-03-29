package OurPlanner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class StateActionState {

	public Set<String> pre = null;
	public String action = null;
	public String actionOwner = null;
	public Set<String> post = null;
	public int traceNumber = 0;

	public StateActionState(StateActionState toCopy) {

		this.pre = new HashSet<String>(toCopy.pre);
		this.action = new String(toCopy.action);
		this.actionOwner = new String(toCopy.actionOwner);
		this.post = new HashSet<String>(toCopy.post);
		this.traceNumber = toCopy.traceNumber;
	}

	public StateActionState(Set<String> pre, String action, String actionOwner, Set<String> post, int traceNumber) {

		this.pre = new HashSet<String>(pre);
		this.action = new String(action);
		this.actionOwner = new String(actionOwner);
		this.post = new HashSet<String>(post);
		this.traceNumber = traceNumber;
	}


	public StateActionState(Set<String> pre, String action, String actionOwner, Set<String> post) {

		this.pre = new HashSet<String>(pre);
		this.action = new String(action);
		this.actionOwner = new String(actionOwner);
		this.post = new HashSet<String>(post);
		this.traceNumber = -1;
	}

	public StateActionState(String sas) {
		if (sas.startsWith("StateActionState")) {

			reconstructPre(sas);

			reconstructAction(sas);

			reconstructActionOwner(sas);

			reconstructPost(sas);

			reconstructTraceNumber(sas);
		}
	}

	private void reconstructPre(String sas) {
		String preStart = "pre[ ";

		int preStartInd = sas.indexOf(preStart) + preStart.length();			
		int preEndInd = sas.indexOf(" ]",preStartInd);

		String[] pre = sas.substring(preStartInd,preEndInd).split(";");
		this.pre = new HashSet<String>();

		for (String str : pre) {
			this.pre.add(str.trim());
		}
	}

	private void reconstructAction(String sas) {
		String actionStart = "action[ ";

		int actionStartInd = sas.indexOf(actionStart) + actionStart.length();			
		int actionEndInd = sas.indexOf(" ]",actionStartInd);

		this.action = sas.substring(actionStartInd,actionEndInd);
	}

	private void reconstructActionOwner(String sas) {
		String actionOwnerStart = "actionOwner[ ";

		int actionOwnerStartInd = sas.indexOf(actionOwnerStart) + actionOwnerStart.length();			
		int actionOwnerEndInd = sas.indexOf(" ]",actionOwnerStartInd);

		this.actionOwner = sas.substring(actionOwnerStartInd,actionOwnerEndInd);
	}

	private void reconstructPost(String sas) {
		String postStart = "post[ ";

		int postStartInd = sas.indexOf(postStart) + postStart.length();			
		int postEndInd = sas.indexOf(" ]",postStartInd);

		String[] post = sas.substring(postStartInd,postEndInd).split(";");
		this.post = new HashSet<String>();

		for (String str : post) {
			this.post.add(str.trim());
		}
	}

	private void reconstructTraceNumber(String sas) {
		String traceNumStart = "traceNum[ ";

		int traceNumStartnStartInd = sas.indexOf(traceNumStart) + traceNumStart.length();			
		int traceNumStartEndInd = sas.indexOf(" ]",traceNumStartnStartInd);

		this.traceNumber = Integer.parseInt(sas.substring(traceNumStartnStartInd,traceNumStartEndInd));
	}

	@Override
	public String toString() {

		String res = "StateActionState [ ";

		res+= "pre[ ";

		Iterator<String> it = pre.iterator();

		while(it.hasNext()) {
			res+= it.next().toString();
			if(it.hasNext())
				res += " ; ";
			else
				res += " ] ";
		}

		res += " ; action[ " + action + " ] ";

		res += " ; actionOwner[ " + actionOwner + " ] ";

		res += " ; post[ ";
		it = post.iterator();

		while(it.hasNext()) {
			res+= it.next().toString();
			if(it.hasNext())
				res += " ; ";
			else
				res += " ] ";
		}

		res += " ; traceNum[ " + traceNumber + " ] ";

		res += "]";

		return res;

	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((actionOwner == null) ? 0 : actionOwner.hashCode());
		result = prime * result + ((post == null) ? 0 : post.hashCode());
		result = prime * result + ((pre == null) ? 0 : pre.hashCode());
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
		StateActionState other = (StateActionState) obj;
		if (action == null) {
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		if (actionOwner == null) {
			if (other.actionOwner != null)
				return false;
		} else if (!actionOwner.equals(other.actionOwner))
			return false;
		if (post == null) {
			if (other.post != null)
				return false;
		} else if (!post.equals(other.post))
			return false;
		if (pre == null) {
			if (other.pre != null)
				return false;
		} else if (!pre.equals(other.pre))
			return false;
		return true;
	}


}
