package cz.agents.alite.communication.zeromq.experimental;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import cz.agents.alite.communication.DefaultPerformerCommunicator;
import cz.agents.alite.communication.Message;
import cz.agents.alite.communication.content.binary.BinaryContent;
import cz.agents.alite.communication.content.binary.BinaryMessageHandler;
import cz.agents.alite.communication.zeromq.ZeroMQCommunicationChannel;
import cz.agents.alite.communication.zeromq.ZeroMQCommunicationChannel.ReceiverTable;

public class AliteAgent{
	
	private final static Logger LOGGER = Logger.getLogger(AliteAgent.class);
	
	public final String id;
//	private final String address;
	private final ReceiverTable directory;
	private final DefaultPerformerCommunicator comm;
	
	private final Timer timer = new Timer();
	private boolean closed = false;
	
	public int received = 0;
	
	
	public AliteAgent(final String id, String address, ReceiverTable directory) {
		super();
		this.id = id;
//		this.address = address;
		this.directory = directory;
		
		LOGGER.info(id + " INIT COMMUNICATOR");
		
		comm = new DefaultPerformerCommunicator(id);
		comm.addPerformerChannel(new ZeroMQCommunicationChannel(comm,id,address,directory));
		
		comm.addMessageHandler(new BinaryMessageHandler() {
			
			
			public void handleMessage(Message message, BinaryContent content) {
				
				String msg = new String(content.getData());
				
				LOGGER.info(id + " received msg from " + message.getSender() + " content: " + msg);
				
				++received;
				
				waitRandom(500,500);
				
				sendMessage("Re:"+msg,message.getSender());
				
			}
		});
		
		
		
	}
	
	public void initConversation() {
		LOGGER.info(id + " SCHEDULE RECEIVER");
		
		timer.scheduleAtFixedRate(new TimerTask() {
			
			
			public void run() {
				if(!closed)comm.performReceiveNonblock();
			}
		}, 0, 100);
		
//		waitRandom(100,0);
		
		LOGGER.info(id + " INIT CONVERSATION");
		for(String rec : directory.getAvailableReceiverIDs()){
			if(!rec.equals(id)){
				sendMessage("Hi from " + id, rec);
				waitRandom(100,100);
			}
		}
		
	}
	
	private void sendMessage(String msg, String rec){
		if(closed)return;
		LOGGER.info(id + " send msg to " + rec + " content: " + msg);
		
		Message message = comm.createMessage(new BinaryContent(msg.getBytes()));
		message.addReceiver(rec);
		comm.sendMessage(message);
	}

	public void waitRandom(int w, int r){
		try {
			Thread.sleep((int)(w + r * Math.random()));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close(){
		closed = true;
		comm.performClose();
	}

	
	
	

}
