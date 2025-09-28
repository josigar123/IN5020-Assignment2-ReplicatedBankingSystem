import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageDeliveryServiceImpl implements MessageDeliveryService{

    // A map of coordinators for multiple groups
    private final Map<String, TransactionCoordinator> coordinators;
    private final String REGISTRY_IP = "localhost";
    private final int REGISTRY_PORT = 1099;
    private final Registry registry;

    public MessageDeliveryServiceImpl(ConcurrentHashMap<String, TransactionCoordinator> coordinators) throws RemoteException {
        this.coordinators = coordinators;
        registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
    }

    @Override
    public Map<String, Double> joinGroup(String groupName, String bankServer) throws RemoteException {

        try{
            // Get the coordinator for the group
            TransactionCoordinator coordinator = coordinators.get(groupName);

            BankService bank = (BankService) registry.lookup(bankServer);
            BankServerInfo bankServerInfo = new BankServerInfo(bank, bankServer);

            coordinator.getGroup().join(bankServerInfo);

            return coordinator.getGroup().getCurrentBalance(); // bank sets its balance to this value if present
        }
        catch(NotBoundException e){
            System.err.println(e.getMessage());
            return null;
        }
    }

    @Override
    public void leaveGroup(String groupName, String bankServer) throws RemoteException {
        Group group = coordinators.get(groupName).getGroup();
        group.leave(bankServer);
    }

    @Override
    public void sendTransactions(String groupName, List<Transaction> transactions) throws RemoteException {
        List<OrderedTransaction> orderedTransactions = coordinators.get(groupName).setTransactionOrder(transactions);
        coordinators.get(groupName).broadCastTransactions(orderedTransactions);
    }

    @Override
    public int getNumberOfMembers(String groupName) throws RemoteException {
        return coordinators.get(groupName).getGroup().getMembers().size();
    }

    @Override
    public List<String> getMemberNames(String groupName) throws RemoteException {
        return coordinators.get(groupName).getGroup().getMembersByNames();
    }
}
