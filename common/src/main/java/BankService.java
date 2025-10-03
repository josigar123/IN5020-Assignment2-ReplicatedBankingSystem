import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// RMI method interface for banking replication consistency
public interface BankService extends Remote {
    boolean deliverOrderedBatch(List<Transaction> orderedTransactions) throws RemoteException;
    Pair getBalance() throws RemoteException;

}
