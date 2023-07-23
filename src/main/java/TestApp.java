import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class TestApp {

    public static void main(String[] args) throws NotBoundException, RemoteException, InterruptedException {

        if (args.length < 2)
        {
            System.out.println("Wrong Format! Must be: TestApp <SubID/PubID> <Operation>");
            return;
        }

        if (args[0].contains("//")){
            System.out.println("ID cannot contain '//'\n");
            return;
        }

        String rmiid = args[0];

        switch (args[1]) {
            case "get":
            {
                if(args.length < 3){
                    System.out.println("Wrong Format! Must be: TestApp <SubID/PubID> get <topic>");
                    return;
                }

                if (args[2].contains("//")){
                    System.out.println("Topic cannot contain '//'\n");
                    return;
                }

                SubscriberInterface subscriber;
                try{
                    subscriber = (SubscriberInterface) LocateRegistry.getRegistry().lookup(rmiid);
                } catch (NotBoundException nb){
                    System.out.println("Subscriber with id '" + rmiid + "' not found in the registry!");
                    return;
                } catch (RemoteException r){
                    System.out.println("Communication with the RMI Registry failed. Check if RMI registry has been correctly started.");
                    return;
                }

                subscriber.Get(args[2]);
                break;
            }
            case "put":
            {
                if(args.length < 4){
                    System.out.println("Wrong Format! Must be: TestApp <SubID/PubID> put <topic> <message>");
                    return;
                }

                if (args[2].contains("//") || args[3].contains("//")){
                    System.out.println("Topic and Message cannot contain '//'\n");
                    return;
                }

                PublisherInterface publisher;
                try{
                    publisher = (PublisherInterface) LocateRegistry.getRegistry().lookup(rmiid);
                } catch (NotBoundException nb){
                    System.out.println("Publisher with id '" + rmiid + "' not found in the registry!");
                    return;
                } catch (RemoteException r){
                    System.out.println("Communication with the RMI Registry failed. Check if RMI registry has been correctly started.");
                    return;
                }
                publisher.Put(args[2], args[3]);
                break;
            }
            case "subscribe":
            {
                if(args.length < 3){
                    System.out.println("Wrong Format! Must be: TestApp <SubID/PubID> subscribe <topic>");
                    return;
                }

                if (args[2].contains("//")){
                    System.out.println("Topic cannot contain '//'\n");
                    return;
                }

                SubscriberInterface subscriber;
                try{
                    subscriber = (SubscriberInterface) LocateRegistry.getRegistry().lookup(rmiid);
                } catch (NotBoundException nb){
                    System.out.println("Subscriber with id '" + rmiid + "' not found in the registry!");
                    return;
                } catch (RemoteException r){
                    System.out.println("Communication with the RMI Registry failed. Check if RMI registry has been correctly started.");
                    return;
                }

                subscriber.Subscribe(args[2]);
                break;
            }
            case "unsubscribe":
            {
                if(args.length < 3){
                    System.out.println("Wrong Format! Must be: TestApp <SubID/PubID> unsubscribe <topic>");
                    return;
                }

                if (args[2].contains("//")){
                    System.out.println("Topic cannot contain '//'\n");
                    return;
                }

                SubscriberInterface subscriber;
                try{
                    subscriber = (SubscriberInterface) LocateRegistry.getRegistry().lookup(rmiid);
                } catch (NotBoundException nb){
                    System.out.println("Subscriber with id '" + rmiid + "' not found in the registry!");
                    return;
                } catch (RemoteException r){
                    System.out.println("Communication with the RMI Registry failed. Check if RMI registry has been correctly started.");
                    return;
                }

                subscriber.Unsubscribe(args[2]);
                break;
            }
            case "topics":
                if(args.length > 2){
                    System.out.println("Wrong Format! Must be: TestApp <SubID/PubID> topics");
                    return;
                }
                SubscriberInterface subscriber;
                try{
                    subscriber = (SubscriberInterface) LocateRegistry.getRegistry().lookup(rmiid);
                } catch (NotBoundException nb){
                    System.out.println("Subscriber with id '" + rmiid + "' not found in the registry!");
                    return;
                } catch (RemoteException r){
                    System.out.println("Communication with the RMI Registry failed. Check if RMI registry has been correctly started.");
                    return;
                }
                subscriber.Topics();
                break;
            default:
            {
                System.out.println("Wrong Format! Available operations are 'get', 'put', 'subscribe' and 'unsubscribe'.");
                return;
            }
        }
    }
}