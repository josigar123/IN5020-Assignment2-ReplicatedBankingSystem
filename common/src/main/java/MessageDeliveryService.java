import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.OptionalInt;

// RMI methods for interacting with the MDS
public interface MessageDeliveryService extends Remote {
    OptionalInt joinGroup(String groupName, String bankServer) throws RemoteException;
    void leaveGroup(String groupName, String bankServer) throws RemoteException;
    void sendTransactions(List<Transaction> transactions) throws RemoteException;
    int getNumberOfMembers(String groupName) throws RemoteException;
}
