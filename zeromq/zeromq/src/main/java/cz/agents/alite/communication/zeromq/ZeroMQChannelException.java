package cz.agents.alite.communication.zeromq;

import cz.agents.alite.communication.channel.CommunicationChannelException;

public class ZeroMQChannelException extends CommunicationChannelException {

	private static final long serialVersionUID = -1839935158584457909L;
	
	String msg="";
	
	public ZeroMQChannelException(String msg){
		this.msg = msg;
	}
	
	
	public String getMessage(){
		return msg;
	}

}
