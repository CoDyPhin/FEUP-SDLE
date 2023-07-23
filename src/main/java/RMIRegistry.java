import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIRegistry {
    public static void main(String[] args) {
        try{
        int port = 1099;
        Registry rmiRegistry = LocateRegistry.createRegistry(port);
        System.out.println("RMI running on port " + port);
        } catch (RemoteException e) {
            System.out.println("There was a problem starting RMI in that port");
            return;
        }
        while(true){}
    }
}
