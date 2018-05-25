package cz.agents.alite.communication.zeromq.experimental;

import java.util.Map;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;



public class SimpleAgent implements Runnable{

    private final static Logger LOGGER = Logger.getLogger(SimpleAgent.class);

    private final Map<String,String> directory;
    private final String id;

    Context context;
    Socket router;

    public boolean waiting = false;
    public boolean loop = true;

    public int received = 0;


    public SimpleAgent(String id,String address, Map<String, String> directory) {
        super();
        this.directory = directory;
        this.id = id;

        context = ZMQ.context(1);
        router = context.socket(ZMQ.ROUTER);
        router.setRouterMandatory(true);
        router.setIdentity(id.getBytes());
        router.bind(address);

        directory.put(id, address);

    }

    public void connectAll(){
        for(String otherid : directory.keySet()){
            if(!otherid.equals(id)){
                connectTo(otherid);
            }
        }
    }

    public void connectTo(String otherid){
        router.connect(directory.get(otherid));
        LOGGER.info(id + " connected to " + otherid + "("+directory.get(otherid)+")");

    }

    public void broadcast(){
        for(String otherid : directory.keySet()){
            if(!otherid.equals(id)){
                sendMessage(otherid);
            }
        }
    }

    public void sendMessage(String to) {

        ZMsg msg = ZMsg.newStringMsg(to, "Hello from " + id);
        LOGGER.info(id + " send Hello to " + to);
        msg.send(router, true);

    }

    public void run() {
        LOGGER.info(id + " receive msgs...");

        while (loop) {
            waiting = true;
            ZMsg msg = ZMsg.recvMsg(router,ZMQ.DONTWAIT);
            waiting = false;

            if(msg!=null && msg.size()>=2){
                String address = msg.pop().toString();
                String content = msg.pop().toString();
                assert (content != null);
                msg.destroy();

                LOGGER.info(id + " received msg from " + address + ", content: " + content);
                ++received;
            }

        }

        router.close();
        context.term();

        LOGGER.info(id + " terminated");
    }



}
