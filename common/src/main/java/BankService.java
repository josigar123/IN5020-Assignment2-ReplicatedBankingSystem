import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// RMI method interface for banking replication consistency
public interface BankService extends Remote {

    // For delivering transactions to the bank
    boolean deliverOrderedBatch(List<Transaction> orderedTransactions) throws RemoteException;

    // for getting a snapshot of the balance for banks joining when a view already exists
    Pair getBalance() throws RemoteException;

}
