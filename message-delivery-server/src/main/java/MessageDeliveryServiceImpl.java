import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageDeliveryServiceImpl extends UnicastRemoteObject implements MessageDeliveryService{

    // A map of coordinators for multiple groups
    private final Map<String, TransactionCoordinator> coordinators;

    // Registry location, change accordingly
    private final String REGISTRY_IP = "localhost";
    private final int REGISTRY_PORT = 1099;

    private final Registry registry;
    private CountDownLatch latch = null;
    private final ReentrantLock lock = new ReentrantLock();
    private final Lock latchLock = new ReentrantLock();
    private boolean newBatch = true;

    public MessageDeliveryServiceImpl(ConcurrentHashMap<String, TransactionCoordinator> coordinators) throws RemoteException {
        super();
        this.coordinators = coordinators;
        // Get the registry
        registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
    }

    @Override
    public Pair joinGroup(String groupName, String bankServer, int numberOfReplicas) throws RemoteException {

        try{
            // Create singualr access for creating a countdown latch, setting the newBatch flag to false (since a batch is being integrated)
            latchLock.lock();
            if(newBatch){
                latch = new CountDownLatch(numberOfReplicas);
                newBatch = false;
            }
            // Release the lock

            latchLock.unlock();

            // Get the coordinator for the group
            TransactionCoordinator coordinator = coordinators.get(groupName);

            // Lookup the joining bank in the registry and create a bankserverinfo object
            BankService bank = (BankService) registry.lookup(bankServer);
            BankServerInfo bankServerInfo = new BankServerInfo(bank, bankServer);

            // Join the coordinators group
            coordinator.getGroup().join(bankServerInfo);
            System.out.println("[MDS] Bank ´" + bankServer + "´ joined group: " + groupName);
            // Countdown the created latch, signifying that bank i has been integrated
            latch.countDown();

            // Return the snapshot if it exists for synch on new joins
            return coordinator.getGroup().getCurrentBalance(); // bank sets its balance to this value if present
        }
        catch(NotBoundException e){
            System.err.println(e.getMessage());
            // Release lock upon failure
            latchLock.unlock();
            return null;
        }
    }

    @Override
    public void leaveGroup(String groupName, String bankServer) throws RemoteException {
        // Remove the bank from the group
        Group group = coordinators.get(groupName).getGroup();
        group.leave(bankServer);
        System.out.println("[MDS] Bank '" + bankServer + "' left group: " + groupName );
    }

    @Override
    public void sendTransactions(String groupName, List<Transaction> transactions) throws RemoteException {

        // Lock when a bank wants to send transactions
        lock.lock();
        try{
            // Set the total order view of the received transactions (appended, order is implied by iterating over list)
            List<Transaction> orderedTransactions = coordinators.get(groupName).setTransactionOrder(transactions);
            System.out.println("[MDS] Broadcasting transactions to group: " + groupName);

            // Broadcast the view to all members of group
            coordinators.get(groupName).broadCastTransactions(orderedTransactions);

            // Remove the getSyncedBalance from the global view
            coordinators.get(groupName).removeGetSyncedBalance();
            
        }finally{
            // Always release the lock
            lock.unlock();
        }
    }

    // Self explanatory
    @Override
    public int getNumberOfMembers(String groupName) throws RemoteException {
        return coordinators.get(groupName).getGroup().getMembers().size();
    }

    // Self explanatory
    @Override
    public List<String> getMemberNames(String groupName) throws RemoteException {
        return coordinators.get(groupName).getGroup().getMembersByNames();
    }

    // Banks call this awaiting the latch, when the latch is zero it means that all banks in the batch
    // are ready to go and can begin executing
    @Override
    public void awaitExecution(){
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            // When the latch frees, then a new batch can be assumed to join on other instantiations of replicas
            newBatch = true;
        }
    }
}
