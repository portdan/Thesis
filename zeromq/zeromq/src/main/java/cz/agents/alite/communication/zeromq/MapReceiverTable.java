package cz.agents.alite.communication.zeromq;

import java.util.Collection;
import java.util.HashMap;

import cz.agents.alite.communication.zeromq.ZeroMQCommunicationChannel.ReceiverTable;

/**
 * ReceiverTable implemented using a HashMap
 * @author stolba
 *
 */
public class MapReceiverTable extends HashMap<String,String> implements ReceiverTable {

	private static final long serialVersionUID = 1970337808263999149L;

	public void addEntry(String id, String address){
		put(id,address);
	}
	
	public boolean containsID(String communicatorID) {
		return containsKey(communicatorID);
	}

	public boolean containsAddress(String communicatorAddress) {
		return containsValue(communicatorAddress);
	}

	public String getAddress(String communicatorID) {
		return get(communicatorID);
	}

	public Collection<String> getAvailableReceiverIDs() {
		return keySet();
	}

}
