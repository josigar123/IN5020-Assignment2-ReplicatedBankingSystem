import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

// RMI methods for interacting with the MDS
public interface MessageDeliveryService extends Remote {
    Map<String, Double> joinGroup(String groupName, String bankServer) throws RemoteException;
    void leaveGroup(String groupName, String bankServer) throws RemoteException;
    void sendTransactions(String groupName, List<Transaction> transactions) throws RemoteException;
    int getNumberOfMembers(String groupName) throws RemoteException;
    List<String> getMemberNames(String groupName) throws RemoteException;
}
