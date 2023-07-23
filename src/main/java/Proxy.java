
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZFrame;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Proxy implements Runnable
{
    private Storage storage = new Storage();

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    private Storage deserializeData(){
        try {
            Storage data;
            FileInputStream fileIn = new FileInputStream("./storage/data.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            data = (Storage) in.readObject();
            in.close();
            fileIn.close();
            return data;
        } catch (FileNotFoundException f){
            return null;
        } catch (IOException i) {
            i.printStackTrace();
            return null;
        } catch (ClassNotFoundException c) {
            System.out.println("Storage class not found");
            c.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args){
        Thread thread = new Thread(new Proxy());
        thread.start();
    }

    @Override
    public void run() {
        Storage newStorage = deserializeData();
        boolean unsubProceed = false;
        if(newStorage != null){
            setStorage(newStorage);
        }
        int msg_id = storage.getMsg_id();
        Runnable serializeData = new Runnable(){
            public void run(){
                try {
                    //FileOutputStream fileOut = new FileOutputStream("/storage/data.ser");
                    File file = new File("./storage/data.ser");
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    FileOutputStream s = new FileOutputStream(file,false);
                    ObjectOutputStream out = new ObjectOutputStream(s);
                    out.writeObject(storage);
                    out.close();
                    s.close();
                } catch (IOException i) {
                    i.printStackTrace();
                }
            }
        };
        ScheduledExecutorService serialize = Executors.newScheduledThreadPool(1);
        serialize.scheduleAtFixedRate(serializeData, 12, 10, TimeUnit.SECONDS);

        Runnable checkGarbageCollection = new Runnable() {
            @Override
            public void run() {
                Map<String, Integer> topics_lastmsgID = new HashMap<>();
                boolean hasSub = false;
                ConcurrentHashMap<String, List<String>> topics_aux = storage.getTopics();
                ConcurrentHashMap<String, Integer> mapping_aux = storage.getMapping();

                for(String topic : topics_aux.keySet()){
                    for(String aux : mapping_aux.keySet()){
                        String[] aux2 = aux.split("//", 2);
                        if(aux2[1].equals(topic)){
                            hasSub = true;
                            if(!topics_lastmsgID.containsKey(topic)) topics_lastmsgID.put(topic, mapping_aux.get(aux));
                            else{
                                if(topics_lastmsgID.get(topic) > mapping_aux.get(aux)){
                                    topics_lastmsgID.replace(topic, mapping_aux.get(aux));
                                }
                            }
                        }

                    }
                    if(!hasSub) {
                        topics_aux.remove(topic);
                        hasSub = true;
                        System.out.println("Deleted topic " + topic + " - No subscribers left");
                    }
                }

                for(String topic : topics_lastmsgID.keySet()){
                    for(String aux : topics_aux.keySet()) {
                        if(aux.equals(topic)){
                            int count = 1;
                            for (int i = 0; i < topics_aux.get(aux).size(); i++) {
                                if(count > topics_lastmsgID.get(topic)) break;
                                topics_aux.get(aux).remove(count);
                                count++;
                            }

                            for (String subid_topic : mapping_aux.keySet()) {
                                String[] aux2 = subid_topic.split("//", 2);
                                if(topic.equals(aux2[1])){
                                    mapping_aux.replace(subid_topic, mapping_aux.get(subid_topic)-count+1);
                                }
                            }
                        }
                    }

                }

                storage.setTopics(topics_aux);
                storage.setMapping(mapping_aux);

            }
        };
        ScheduledExecutorService garbage_collection = Executors.newScheduledThreadPool(1);
        garbage_collection.scheduleAtFixedRate(checkGarbageCollection, 10, 10, TimeUnit.SECONDS);

        try (ZContext context = new ZContext()) {
            Socket xsub = context.createSocket(SocketType.SUB);
            Socket xpub = context.createSocket(SocketType.XPUB);

            Socket reply_socket_pub = context.createSocket(SocketType.REP); //confirm PUB
            Socket subcrash_socket = context.createSocket(SocketType.REP); // send list of topics to subs

            xsub.bind("tcp://*:5559");
            xpub.bind("tcp://*:5560");

            reply_socket_pub.bind("tcp://*:5502");
            subcrash_socket.bind("tcp://*:5503");

            System.out.println("\n- Launched and connected proxy successfully.");
            xsub.subscribe(ZMQ.SUBSCRIPTION_ALL);
            //  Initialize poll set
            Poller items = context.createPoller(3);
            items.register(xpub, Poller.POLLIN); //frontend
            items.register(xsub, Poller.POLLIN); //backend
            items.register(subcrash_socket, Poller.POLLIN);


            while(true){
                items.poll();
                if (items.pollin(0)){ //SUBSCRIBER
                    ZFrame frame = ZFrame.recvFrame(xpub); // from subscriber "subID//topic//ifGET"

                    if(frame == null) break;
                    byte[] message = frame.getData();
                    frame.destroy();

                    String key;
                    String msgRight = new String(message, 1, message.length - 1, ZMQ.CHARSET);
                    String[] msgArray = msgRight.split("//",3);
                    String subId = msgArray[0];
                    String topic = msgArray[1];
                    //System.out.println(msgArray.length);
                    if(msgArray.length > 2){ //GET
                        // recebeu get
                        if(message[0] == 1){ // get via subscribe
                            ConcurrentHashMap<String, List<String>> topics = storage.getTopics();
                            ConcurrentHashMap<String, Integer> mapping = storage.getMapping();
                            if(topics.containsKey(topic)){
                                key = subId+"//"+topic;
                                if(mapping.containsKey(key) ){
                                    List<String> messages = topics.get(topic);
                                    int index = mapping.get(key);
                                    if(index == messages.size()-1){
                                        System.out.println("\nSubscriber "+ subId+"- There are no new messages to read on topic "+topic+".");
                                        xpub.send(subId + "//" + topic + "//" + "No more messages//-1");
                                    }
                                    else if(messages.size()-1 > index){
                                        String messageToSend = messages.get(index + 1);
                                        xpub.send(subId + "//" + topic + "//" + messageToSend);


                                        Thread thread = new Thread(){
                                            public void run(){
                                                Socket request_socket_sub = context.createSocket(SocketType.REQ); //confirm get
                                                request_socket_sub.bind("tcp://*:5501");
                                                if(request_socket_sub.send(subId)){
                                                    System.out.println("\nMessage sent to subscriber "+subId+". Waiting for the confirmation.");
                                                }
                                                String id = waitForResponse(context, request_socket_sub, 3, 5000);
                                                request_socket_sub.close();
                                                if(id!=null){
                                                    if(id.equals(subId)){
                                                        System.out.println("Confirmation received.");
                                                        mapping.put(key, index+1);
                                                    }
                                                }
                                            }
                                        };
                                        thread.start();

                                    }
                                }
                                else{
                                    System.out.println("\nSubscriber " + subId + "! Subscribe to the topic first.");
                                }
                            }
                            else{
                                System.out.println("\nSubscriber " +subId + "! That topic does not exist.");
                            }
                        }
                    }
                    else{
                        switch(message[0]){
                            case 1://SUBSCRIBE
                                key = subId+"//"+topic;
                                ConcurrentHashMap<String, List<String>> topics = storage.getTopics();
                                ConcurrentHashMap<String, Integer> mapping = storage.getMapping();
                                if(topics.containsKey(topic)){
                                    if(mapping.containsKey(key)){
                                        System.out.println("\nThe subscriber " + subId + " was already subscribed to topic " + topic + ".");
                                    }
                                    else {
                                        mapping.put(key, topics.get(topic).size() - 1);
                                        storage.setMapping(mapping);
                                        serializeData.run();
                                        xpub.send(key + "//" + 1);
                                        System.out.println("\nSubscriber " + subId + " successfully subscribed to topic " + topic+ ".");
                                    }
                                }
                                else{
                                    List<String> emptyMessage= new ArrayList<>();
                                    emptyMessage.add("");
                                    topics.put(topic, emptyMessage);
                                    storage.setTopics(topics);
                                    mapping.put(key, 0);
                                    storage.setMapping(mapping);
                                    serializeData.run();
                                    xpub.send(key+"//"+1);
                                    System.out.println("\nSubscriber " + subId + " successfully subscribed to topic " + topic+".");
                                }
                                break;
                            case 0: //UNSUBSCRIBE
                                key = subId+"//"+topic;
                                topics = storage.getTopics();
                                mapping = storage.getMapping();
                                if(unsubProceed){
                                    unsubProceed = false;
                                    if(topics.containsKey(topic)){
                                        if(mapping.containsKey(key)){
                                            mapping.remove(key);
                                            storage.setMapping(mapping);
                                            serializeData.run();
                                            System.out.println("\nSubscriber " + subId + " successfully unsubscribed from topic " + topic+".");
                                        }
                                        else{
                                            System.out.println("\nSubscriber " + subId + " was not subscribed to topic " + topic+".");
                                        }
                                    }
                                    else{
                                        System.out.println("\nSubscriber " +subId + "! That topic does not exist.");
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (items.pollin(1)) { //PUBLISHER
                    String fullMessage = xsub.recvStr();
                    String[] msgArray = fullMessage.split("//", 2);
                    if(msgArray.length >=2){
                        String topic = msgArray[0];
                        String message = msgArray[1];
                        ConcurrentHashMap<String, List<String>> topics = storage.getTopics();
                        if (topics.containsKey(topic)) {
                            List<String> messages = topics.get(topic);
                            messages.add(message+"//"+msg_id);
                            storage.setTopics(topics);
                            msg_id++;
                            storage.setMsg_id(msg_id);
                            serializeData.run();
                        }
                        Thread ackThread = new Thread(){
                            public void run(){
                                String id=waitForResponse(context, reply_socket_pub, 3, 5000);
                                reply_socket_pub.send(id); //Confirmação
                                System.out.println("\nMessage added with success on topic "+topic+". Sending confirmation to publisher " + id+ ".");
                            }
                        };
                        ackThread.start();
                    }
                }
                if (items.pollin(2)){ //send topics to sub
                    String reqSubId = subcrash_socket.recvStr();
                    String[] reqArray = reqSubId.split("//",2);
                    ConcurrentHashMap<String, List<String>> topics = storage.getTopics();
                    ConcurrentHashMap<String, Integer> mapping = storage.getMapping();
                    if (reqArray.length == 2){
                        subcrash_socket.send(reqArray[0]);
                        unsubProceed = true;
                    }
                    else{
                        String subTopics = "";
                        for (String chave : mapping.keySet()) {
                            String[] keyArray = chave.split("//",2);
                            if(keyArray[0].equals(reqSubId)){
                                subTopics = subTopics + "//" + keyArray[1];
                            }
                        }
                        subcrash_socket.send(subTopics);
                    }
                }
            }
        }
    }

    private String waitForResponse(ZContext context, Socket socket , int tries, int timeout){
        Poller items3 = context.createPoller(1);
        items3.register(socket, Poller.POLLIN);
        int count = 0;
        String retval = null;
        while(count < tries){
            items3.poll(timeout);
            if(items3.pollin(0)){

                retval = socket.recvStr();
                break;
            }
            count++;
        }
        if(retval == null) System.out.println("Communication Timed Out. Try again.");
        return retval;
    }
}