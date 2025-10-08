import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// RMI methods for interacting with the MDS
public interface MessageDeliveryService extends Remote {

    // Banks join a group with this method, returns a snapshot if it exists
    Pair joinGroup(String groupName, String bankServer, int numberOfReplicas) throws RemoteException;

    // Bank can leave a group
    void leaveGroup(String groupName, String bankServer) throws RemoteException;

    // Banks can send transaction with this method
    void sendTransactions(String groupName, List<Transaction> transactions) throws RemoteException;

    // can be used for getting the number of members of a group
    int getNumberOfMembers(String groupName) throws RemoteException;

    // Get the names of members of a group
    List<String> getMemberNames(String groupName) throws RemoteException;

    // A call to synchronize with a batch of joining banks
    void awaitExecution() throws RemoteException;
}
