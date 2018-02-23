package cz.agents.alite.communication;

import cz.agents.alite.communication.content.Content;

/**
 * Utility class used to create messages from data received on the wire.
 * 
 * @author stolba
 *
 */
public class MessageCreator {
	
	private static long counter = System.currentTimeMillis();
	
	private final String id;
	
	public MessageCreator(String id) {
		super();
		this.id = id;
	}

	public Message createMessage(String from, Content content){
		return new Message(from,content,generateId());
	}
	
	private long generateId() {
        return id.hashCode() + counter;
    }

}
