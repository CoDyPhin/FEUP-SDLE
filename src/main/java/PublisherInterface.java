import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PublisherInterface extends Remote {
    int Put(String topic, String message) throws RemoteException, InterruptedException;
}
