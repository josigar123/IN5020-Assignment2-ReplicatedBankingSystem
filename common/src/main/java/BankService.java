import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

// RMI method interface for banking replication consistency
public interface BankService extends Remote {
    boolean deliverOrderedBatch(List<OrderedTransaction> orderedTransactions) throws RemoteException;
    void onMembershipChange(String groupName, List<String> currentReplicaIds) throws RemoteException;
    Map<String, Double> getBalance() throws RemoteException;

}
