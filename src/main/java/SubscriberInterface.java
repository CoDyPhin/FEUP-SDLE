import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SubscriberInterface extends Remote {
    int Get(String topic) throws RemoteException;
    int Subscribe(String topic) throws RemoteException;
    int Unsubscribe(String topic) throws RemoteException;
    int Topics() throws RemoteException;
}
