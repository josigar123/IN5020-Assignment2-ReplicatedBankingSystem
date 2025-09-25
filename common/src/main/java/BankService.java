import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// RMI method interface for banking replication consistency
public interface BankService extends Remote {
    void deliverOrderedBatch(String groupName, long sequence, List<Transaction> transactions) throws RemoteException;
    void onMembershipChange(String groupName, List<String> currentReplicaIds) throws RemoteException;

}
