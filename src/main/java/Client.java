import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;

public class Client {
    public static void main(String[] args) throws RemoteException{
        if(args.length < 1){
            System.out.println("Please specify if you wish to connect a pub or a sub");
            return;
        }
        if(args.length < 2){
            System.out.println("Please specify an (unique) ID for this client");
            return;
        }
        else{
            if (args[1].contains("//")){
                System.out.println("ID cannot contain '//'\n");
                return;
            }
        }
        switch (args[0]){
            case "pub":
                Publisher pub = new Publisher(args[1]);
                try {/*
                    Registry rmiRegistry = LocateRegistry.createRegistry(1099);
                    PublisherInterface stub = (PublisherInterface) UnicastRemoteObject.exportObject(pub, 0);
                    rmiRegistry.rebind(pub.getPubId(), stub);
                    System.out.printf("\n- Publisher %s is ready.\n", pub.getPubId());
                } catch (ExportException f) {*/

                    Registry rmiRegistry = LocateRegistry.getRegistry(1099);
                    PublisherInterface stub = (PublisherInterface) UnicastRemoteObject.exportObject(pub, 0);
                    rmiRegistry.rebind(pub.getPubId(), stub);
                    System.out.printf("\n- Publisher %s ready.\n", pub.getPubId());
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case "sub":
                Subscriber sub = new Subscriber(args[1]);
                try {/*
                    Registry rmiRegistry = LocateRegistry.createRegistry(1099);
                    SubscriberInterface stub = (SubscriberInterface) UnicastRemoteObject.exportObject(sub, 0);
                    rmiRegistry.rebind(sub.getSubId(), stub);
                    System.out.printf("\n- Subscriber %s is ready.\n", sub.getSubId());
                } catch (ExportException f) {*/
                    Registry rmiRegistry = LocateRegistry.getRegistry(1099);
                    SubscriberInterface stub = (SubscriberInterface) UnicastRemoteObject.exportObject(sub, 0);
                    rmiRegistry.rebind(sub.getSubId(), stub);
                    System.out.printf("\n- Subscriber %s is ready.\n", sub.getSubId());
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("Wrong argument format! Please use: Client <pub>/<sub> <id>");
                break;
        }
    }
}
