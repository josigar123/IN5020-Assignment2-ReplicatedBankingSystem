import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageDeliveryServer {

    private static final String REGISTRY_IP = "localhost";
    private static final int REGISTRY_PORT = 1099;

    private List<OrderedTransaction> orderedTransactions;
    private List<BankServerInfo> bankServers;
    private AtomicInteger orderCounter = new AtomicInteger(0);

    public void broadCastTransactions(){

        for(BankServerInfo bankServerInfo : bankServers){

            // Get the bank stub
            BankService bank = bankServerInfo.getBank();

        }
    }

    public void evictFailedServer(String bankName){
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setTransactionOrder(){
        throw new UnsupportedOperationException("Not supported yet.");
    }

    void resendTransactionList(){
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void main(String[] args) throws RemoteException {

        // Create the group
        Group group = new Group("group3");

        // When starting server, locate the RMI registry
        Registry registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
        MessageDeliveryServiceImpl service = new MessageDeliveryServiceImpl(group);

        // bind service to registry
        registry.rebind("message-delivery-service", service);


    }
}
