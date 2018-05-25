package cz.agents.alite.communication.zeromq;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import cz.agents.alite.communication.CommunicationReceiver;
import cz.agents.alite.communication.Message;
import cz.agents.alite.communication.MessageCreator;
import cz.agents.alite.communication.channel.CommunicationChannelBroadcast;
import cz.agents.alite.communication.channel.CommunicationChannelException;
import cz.agents.alite.communication.channel.CommunicationPerformerChannel;
import cz.agents.alite.communication.content.binary.BinaryContent;
import cz.agents.alite.communication.zeromq.experimental.SimpleAgent;

/**
 * Communication channel utilizing the 0MQ (jeromq) infrastructure. The connections
 * are realized using a ROUTER-ROUTER pair.
 * 
 * @author stolba
 *
 */
public class ZeroMQCommunicationChannel implements CommunicationPerformerChannel {

	private final static Logger LOGGER = Logger.getLogger(SimpleAgent.class);
	
	private final CommunicationReceiver receiver;
	private final String id;
	private final ReceiverTable directory;
	private Collection<String> broadcastReceivers = null;
	
	private final Context context;
	private final Socket router;
	
	private final MessageCreator creator;
	
	private boolean closedProperly = false;
	private boolean closed = true;
	
	/**
	 * Communication channel utilizing the 0MQ (jeromq) infrastructure. The connections
	 * are realized using a ROUTER-ROUTER pair. When created, the channel binds its
	 * ROUTER socket to the provided address and tries to connect to all (other)
	 * channels in the ReceiverTable.
	 * NOTE: The channels should connect only one-directionally.
	 * 
	 * @param receiver Reciever interface to receive the messages.
	 * @param id ID of the used ROUTER. used for addressing
	 * @param address URL formatted string, see 0MQ documentation for available protocols.
	 * @param directory Receiver table mapping IDs and addresses.
	 */
	public ZeroMQCommunicationChannel(CommunicationReceiver receiver, String id, String address, ReceiverTable directory) {
		super();
		this.receiver = receiver;
		this.id = id;
		this.directory = directory;
		
		creator = new MessageCreator(id);
		
		context = ZMQ.context(1);
	    router = context.socket(ZMQ.ROUTER);
	    router.setRouterMandatory(true);	//throws exception when receiver not available
	    router.setIdentity(id.getBytes());
	    router.bind(address);
	    
	    closed = false;
	    
			
		for(String otherid : directory.getAvailableReceiverIDs()){
			if(!otherid.equals(id)){
				router.connect(directory.getAddress(otherid));
				LOGGER.info(id + " connect to " + otherid + "("+directory.getAddress(otherid)+")");
			}
		}
			
		
	}
	
	private Collection<String> getBroadcastReceivers(){
		if(broadcastReceivers == null){
			broadcastReceivers = new HashSet<String>(directory.getAvailableReceiverIDs());
			broadcastReceivers.remove(id);
		}
		return broadcastReceivers;
	}

	public void sendMessage(Message message) throws CommunicationChannelException {
		if(closed){
			LOGGER.warn(id + " closed channel attempting to send message: " + message);
			return;
		}
		if(!(message.getContent() instanceof BinaryContent)){
			LOGGER.error(id + " - message content not BinaryContent!");
			throw new CommunicationChannelException();
		}
		
		BinaryContent bin = (BinaryContent)message.getContent();
		
		Collection<String> receivers = message.getReceivers().contains(CommunicationChannelBroadcast.BROADCAST_ADDRESS) ? getBroadcastReceivers() : message.getReceivers();
		
		for(String rec : receivers){
//			ZMsg msg = ZMsg.newStringMsg(rec, new String(bin.getData()));
			ZMsg msg = new ZMsg();
			msg.push(new ZFrame(bin.getData()));
			msg.push(new ZFrame(rec));
			if(LOGGER.isDebugEnabled())LOGGER.debug(id + " send msg to " + rec + ", content:"+Arrays.toString(bin.getData()));
			try{
				msg.send(router, true);
			}catch(ZMQException e){
				if(e.getErrorCode() == 65){
					LOGGER.error(id + " no connection to receiver " + rec, e);
					throw new ZeroMQChannelException(id + " no connection to receiver " + rec);
				}else{
					LOGGER.error(id + " exception sending msg to " + rec, e);
					throw new ZeroMQChannelException(id + " no connection to receiver " + rec);
				}
				
			}
		}
	}

	public void receiveMessage(Message message) {
		receiver.receiveMessage(message);
	}

	public boolean performReceiveNonblock() {
		if(closed)return false;
		
		ZMsg msg = ZMsg.recvMsg(router,ZMQ.DONTWAIT);
		
		boolean received = false;
		
		if(msg.size()>=2){
            String senderid = msg.pop().toString();
            byte[] content = msg.pop().getData();
            assert (content != null);
            msg.destroy();
            
            if(LOGGER.isDebugEnabled())LOGGER.debug(id + " received msg from " + senderid + ", content: " + Arrays.toString(content));
            
            receiveMessage(creator.createMessage(senderid,new BinaryContent(content)));
            
            received = true;
		}
		
		return received;
	}
	
	
	public void performReceiveBlock(long timeoutMs) {
		if(closed)return;
		
		ZMsg msg = ZMsg.recvMsg(router);
		
		if(msg.size()>=2){
            String senderid = msg.pop().toString();
            byte[] content = msg.pop().getData();
            assert (content != null);
            msg.destroy();
            
            if(LOGGER.isDebugEnabled())LOGGER.debug(id + " received msg from " + senderid + ", content: " + Arrays.toString(content));
            
            receiveMessage(creator.createMessage(senderid,new BinaryContent(content)));
            
		}
	
    }
	
	public void performClose(){
		LOGGER.warn(id + " close connections");
		if(closedProperly){
			LOGGER.warn(id + " attempting to close channel which is already closed");
			return;
		}
		if(closed){
			LOGGER.warn(id + " attempting to close channel which is already being closed");
		}
		
		closed = true;
		
		for(String otherid : directory.getAvailableReceiverIDs()){
			if(!otherid.equals(id)){
				router.disconnect(directory.getAddress(otherid));
				LOGGER.info(id + " disconnect " + otherid + "("+directory.getAddress(otherid)+")");
			}
		}
		
		router.close();
        context.term();
        
        closedProperly = true;
	}
	
	
	public void finalize(){
		if(!closedProperly){
			LOGGER.warn(id + " connections not closed properly! You must call the closeConnections() method!");
			router.close();
	        context.term();
		}
	}
	
	

	
	public interface ReceiverTable {

        public boolean containsID(String communicatorID);
        
        public boolean containsAddress(String communicatorAddress);

        public String getAddress(String communicatorID);
        
//        public String getID(String communicatorAddress);
        
        public Collection<String> getAvailableReceiverIDs();

	
	}
}
