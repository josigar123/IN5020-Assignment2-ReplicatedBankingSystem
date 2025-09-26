import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// RMI methods for interacting with the MDS
public interface MessageDeliveryService extends Remote {
    void joinGroup(String groupName, String bankServer) throws RemoteException;
    void leaveGroup(String groupName, String bankServer) throws RemoteException;
    void sendTransactions(List<Transaction> transactions) throws RemoteException;
    void ackBroadcast(String bankName, int broadCastId) throws RemoteException;
}
