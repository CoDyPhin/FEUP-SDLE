import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

public class Publisher implements PublisherInterface
{
    private ZMQ.Socket pubsocket;
    private ZMQ.Socket request_socket; //confirm
    private final String pubId;

    public String getPubId() {
        return pubId;
    }

    private final ZContext context = new ZContext();

    public Publisher(String pubId) {
        //  Socket to talk to server
        this.pubId = pubId;
        pubsocket = context.createSocket(SocketType.PUB);
        pubsocket.connect("tcp://localhost:5559");

        request_socket = context.createSocket(SocketType.REQ);
        request_socket.connect("tcp://localhost:5502");
        request_socket.base().setSocketOpt(zmq.ZMQ.ZMQ_REQ_RELAXED, 1);
    }

    @Override
    public int Put(String topic, String message) throws InterruptedException {
        System.out.printf(
                "\nPublishing to topic '%s' the message: '%s'.\n", topic, message
        );
        pubsocket.send(topic + "//" + message);
        ZMQ.Poller items = context.createPoller(1);
        items.register(request_socket, ZMQ.Poller.POLLIN);
        int count = 0;
        request_socket.send(pubId);
        System.out.println("Asking for confirmation from proxy.");
        while(count < 3){
            items.poll(5000);
            if(items.pollin(0)){
                String id = request_socket.recvStr();
                if(id.equals(pubId)){
                     System.out.println("Confirmation received. Successfully published.");
                     return 0;
                }
                break;
            }
            count++;
        }

        if(count == 3){
            System.out.println("Connection failed.");
        }

        return 1;
    }
}