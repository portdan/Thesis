package OurPlanner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class StateActionState {

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

	public StateActionState(String sas) {
		if (sas.startsWith("StateActionState")) {

			reconstructPre(sas);

			reconstructAction(sas);

			reconstructActionOwner(sas);

			reconstructPost(sas);
		}
	}

	private void reconstructPre(String sas) {
		String preStart = "pre[ ";

		int preStartInd = sas.indexOf(preStart) + preStart.length();			
		int preEndInd = sas.indexOf("]",preStartInd);

		String[] pre = sas.substring(preStartInd,preEndInd).split(";");
		this.pre = new HashSet<String>();

		for (String str : pre) {
			this.pre.add(str.trim());
		}
	}

	private void reconstructAction(String sas) {
		String actionStart = "action[ ";

		int actionStartInd = sas.indexOf(actionStart) + actionStart.length();			
		int actionEndInd = sas.indexOf("]",actionStartInd);

		this.action = sas.substring(actionStartInd,actionEndInd);
	}

	private void reconstructActionOwner(String sas) {
		String actionOwnerStart = "actionOwner[ ";

		int actionOwnerStartInd = sas.indexOf(actionOwnerStart) + actionOwnerStart.length();			
		int actionOwnerEndInd = sas.indexOf("]",actionOwnerStartInd);

		this.actionOwner = sas.substring(actionOwnerStartInd,actionOwnerEndInd);
	}

	private void reconstructPost(String sas) {
		String postStart = "post[ ";

		int postStartInd = sas.indexOf(postStart) + postStart.length();			
		int postEndInd = sas.indexOf("]",postStartInd);

		String[] post = sas.substring(postStartInd,postEndInd).split(";");
		this.post = new HashSet<String>();

		for (String str : post) {
			this.post.add(str.trim());
		}
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

		res += "]";

		return res;

	}
}
