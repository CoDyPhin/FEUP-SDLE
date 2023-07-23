import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;


public class Subscriber implements SubscriberInterface
{
    private ZMQ.Socket subsocket;

    private ZMQ.Socket request_socket; //confirmation

    private List<String> subscriptions = new ArrayList();
    private List<Integer> alreadyReceived = new ArrayList();
    private final ZContext context = new ZContext();
    private final String subId;

    public String getSubId() {
        return subId;
    }

    public Subscriber(String SubId) throws RemoteException {
        //  Socket to talk to server
        this.subId = SubId;
        subsocket = context.createSocket(SocketType.SUB);
        subsocket.connect("tcp://localhost:5560");

        request_socket = context.createSocket(SocketType.REQ); //Request list topics from proxy
        request_socket.connect("tcp://localhost:5503");
        request_socket.base().setSocketOpt(zmq.ZMQ.ZMQ_REQ_RELAXED, 1);
        Thread thread = new Thread(){
            public void run(){
                request_socket.send(subId);
                String listTopics = waitForResponse(context, request_socket, 3, 5000);
                if(listTopics == null){
                    return;
                }

                String[] arr=listTopics.split("//");
                for (String topic : arr){
                    if(topic!=null&&!topic.equals("")){
                        subscriptions.add(topic);
                        System.out.println("Re-subscribing to topic " + topic + ".");
                        subsocket.subscribe((subId + "//" + topic).getBytes(ZMQ.CHARSET)); // subID//topic//ifGET
                    }
                }
            }
        };
        thread.start();
    }
    @Override
    public int Topics() {
        if(subscriptions.isEmpty()){
            System.out.println("\nSubscriber "+subId+" didn't subscribe any topics.");
        }
        for (String topic : subscriptions){
            System.out.println("\nSubscriber "+subId+" is subscribed the following topics:");
            System.out.println(" - "+topic);
        }
        return 0;
    }

    @Override
    public int Get(String topic) {
        // Message:  subscribe but with //get in the end

        if(subscriptions.contains(topic)) {
            System.out.println("\nReading messages on topic '" + topic + "'...");
            String messageToSend = subId + "//" + topic + "//" + "get";
            subsocket.subscribe(messageToSend.getBytes(ZMQ.CHARSET)); //subscription to GET message
            return pollResponse(messageToSend, false);
        }
        else{
            System.out.println("\nGet - This subscriber is not subscribed to the topic "+topic+".");
            return 1;
        }
    }

    @Override
    public int Subscribe(String topic) {
        // Message: "subID//topic//ifGET"

        if(!subscriptions.contains(topic)) {
            System.out.println("\nSubscribing to topic " + topic + "...");
            subsocket.subscribe((subId + "//" + topic).getBytes(ZMQ.CHARSET)); // subID//topic//ifGET
            return pollResponse(topic, true);
        }
        else{
            System.out.println("\nThe topic " + topic+" has already been subscribed.");
            return 1;
        }
    }

    @Override
    public int Unsubscribe(String topic) {
        // Message: "subID//unsubscribe topic"

        if(subscriptions.contains(topic)){
            System.out.println("\nUnsubscribing to topic " + topic+"...");
            request_socket.send(subId+"//unsub");
            String id = waitForResponse(context, request_socket, 3, 5000);
            if(id!=null){
                if(id.equals(subId)){
                    subscriptions.remove(topic);
                }
            }
            else System.out.println("Connection to server failed, unsubscribing locally anyway.");
            subsocket.unsubscribe((subId + "//" + topic).getBytes(ZMQ.CHARSET));
            System.out.println("Unsubscription successful.");
            //return pollResponse("unsub", topic, true);
            return 0;
        }
        else {
            System.out.println("\nUnsubscribe - The topic "+ topic+" has not been subscribed yet.");
            return 1;
        }
    }

    private int pollResponse(String str, boolean sub) {
        ZMQ.Poller items = context.createPoller(1);
        items.register(subsocket, ZMQ.Poller.POLLIN);
        int count = 0;
        String response;
        while(count < 20) {
            items.poll(500);
            if (items.pollin(0)) {
                response = subsocket.recvStr();
                String[] msgArray = response.split("//", 4); //subId topic msg msgID
                String msg = msgArray[2];
                if(sub){
                    if(msg.equals("1")){
                        subscriptions.add(str);
                        System.out.println("Successfully subscribed.");
                        return 0;
                    }
                    else{
                        System.out.println("Unknown response received.");
                        return 1;
                    }
                }
                else{ //get
                    Integer identifier = Integer.parseInt(msgArray[3]);
                    if(identifier==-1 && msg.equals("No more messages")){
                        System.out.println("No more messages to read on topic " + msgArray[1] + ".");
                    }
                    else {
                        if (!alreadyReceived.contains(identifier)) {
                            System.out.println("Received message: " + msg + ".");
                            alreadyReceived.add(identifier);
                        }
                        Thread ackThread = new Thread() {
                            public void run() {
                                ZMQ.Socket reply_socket = context.createSocket(SocketType.REP);
                                reply_socket.connect("tcp://localhost:5501");
                                String id = waitForResponse(context, reply_socket, 3, 5000);
                                if (id != null) {
                                    if (id.equals(subId)) {
                                        reply_socket.send(subId);
                                        System.out.println("Sent confirmation to Proxy acknowledging reception.");
                                    }
                                }
                                reply_socket.close();
                            }
                        };
                        ackThread.start();
                    }
                    subsocket.unsubscribe(str.getBytes(ZMQ.CHARSET));
                    return 0;
                }
            }
            count++;
        }
        if(sub) System.out.println("There was a problem subscribing.");
        else {
            System.out.println("No messages found to read.");
            subsocket.unsubscribe(str.getBytes(ZMQ.CHARSET));
            return 0;
        }
        return 1;
    }

    private static String waitForResponse(ZContext context, ZMQ.Socket socket , int tries, int timeout){
        ZMQ.Poller items = context.createPoller(1);
        items.register(socket, ZMQ.Poller.POLLIN);
        int count = 0;
        String retval = null;
        while(count < tries){
            items.poll(timeout);
            if(items.pollin(0)){
                retval = socket.recvStr();
                break;
            }
            count++;
        }
        if(retval == null) System.out.println("Communication Timed Out. Try again.");
        return retval;
    }
}