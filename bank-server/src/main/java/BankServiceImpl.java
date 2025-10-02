import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BankServiceImpl extends UnicastRemoteObject implements BankService {

    private final String groupName;
    private final Map<String, Double> balanceByCurrency = new ConcurrentHashMap<>();
    private BankRepository repository;

    public BankServiceImpl(String groupName, BankRepository repository) throws RemoteException {
        this.groupName = groupName;
        this.repository = repository;
    }

    @Override
    public boolean deliverOrderedBatch(List<Transaction> orderedTransactions) throws RemoteException {
        throw  new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void onMembershipChange(String group, List<String> currentReplicaIds) throws RemoteException {
        throw  new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, Double> getBalance() throws RemoteException {
        return Map.copyOf(balanceByCurrency);
    }


    public void setInitialBalance(Map<String, Double> initialBalance) throws RemoteException {
      balanceByCurrency.clear();
      balanceByCurrency.putAll(initialBalance);
    }

    
    public BankRepository getRepository() {
        return this.repository;
}

}



