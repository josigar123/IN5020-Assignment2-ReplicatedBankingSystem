import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.OptionalInt;

public class MessageDeliveryServiceImpl implements MessageDeliveryService{

    private final TransactionCoordinator coordinator;
    private final String REGISTRY_IP = "localhost";
    private final int REGISTRY_PORT = 1099;
    private final Registry registry;

    public MessageDeliveryServiceImpl(TransactionCoordinator coordinator) throws RemoteException {
        this.coordinator = coordinator;
        registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
    }

    /*
        If new bank arrives after system start
        Set its balance to the current one
     */
    @Override
    public OptionalInt joinGroup(String groupName, String bankServer) throws RemoteException {

        try{
            BankService bank = (BankService) registry.lookup(bankServer);
            BankServerInfo bankServerInfo = new BankServerInfo(bank, bankServer);

            Group group =  coordinator.getGroup();

            group.join(bankServerInfo);

            return OptionalInt.of(group.getCurrentBalance()); // bank sets its balance to this value if not null
        }
        catch(NotBoundException e){
            System.err.println(e.getMessage());
            return OptionalInt.empty();
        }
    }

    @Override
    public void leaveGroup(String groupName, String bankServer) throws RemoteException {
        Group group = coordinator.getGroup();
        group.leave(bankServer);
    }

    @Override
    public void sendTransactions(List<Transaction> transactions) throws RemoteException {
        coordinator.setTransactionOrder(transactions);
        coordinator.broadCastTransactions();
    }

    @Override
    public int getNumberOfMembers(String groupName) throws RemoteException {
        return coordinator.getGroup().getMembers().size();
    }
}
