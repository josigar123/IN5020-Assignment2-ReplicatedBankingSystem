import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BankServiceImpl extends UnicastRemoteObject implements BankService {

    private final String groupName;
    private final Map<String, Double> balanceByCurrency = new ConcurrentHashMap<>();
    

    public BankServiceImpl(String groupName) throws RemoteException {
        this.groupName = groupName;
    }

    @Override
    public boolean deliverOrderedBatch(List<OrderedTransaction> orderedTransactions) throws RemoteException {
        // implementere her 
    }

    @Override
    public void onMembershipChange(String group, List<String> currentReplicaIds) throws RemoteException {
        if (!this.groupName.equals(group)) return;
        BankServer.setCurrentMembers(currentReplicaIds);
    }

    @Override
    public double getBalance(String group, String currency) throws RemoteException {
        if (!this.groupName.equals(group)) return 0.0;
        return balanceByCurrency.getOrDefault(currency, 0.0);
    }


  public void setInitialBalance(int value, String currency) {
        balanceByCurrency.put(currency, (double) value);
    }
}



