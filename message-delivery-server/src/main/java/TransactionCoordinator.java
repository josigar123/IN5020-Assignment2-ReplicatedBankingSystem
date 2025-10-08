import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

// A class for coordinating transactions for a group and its members
// Usefult class for simplifying development of other aspects of the system
public class TransactionCoordinator {
    private final Group group; // Group to coordinate

    private final AtomicInteger orderCounter = new AtomicInteger(0);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Transaction> transactionView = new CopyOnWriteArrayList<>();

    // Snapshot instantiated to null at first
    private Pair snapshot = null;

    public TransactionCoordinator(Group group) {
        this.group = group;
    }

    // Tries to send messages with retry, after 5 secs assume bank has failed
    private boolean sendWithRetry(BankService bank, List<Transaction> batch) {
        long start = System.currentTimeMillis();

        while(true){

            // Create a future and try to deliver the ordered batch (view) to the bank)
            // each attempt times out after 2 seconds, and quits when  the passed time is 5s
            Future<Boolean> future = executor.submit(() -> bank.deliverOrderedBatch(batch));
            try{
                // wait 2 secs for future to complete
                boolean ack = future.get(2, TimeUnit.SECONDS);
                if(ack){
                    // If it returned then set its snapshot with its balance
                    snapshot = bank.getBalance();

                    // Return true == "ACK"
                    return true;
                }
            }catch(TimeoutException e){
                future.cancel(true);
                System.out.println("[MDS] Timeout, retrying...");
            }catch (Exception e){
                System.out.println(e.getMessage());
            }

            if(System.currentTimeMillis() - start > 5000){
                System.out.println("[MDS] Bank failed after 5s");
                // BAnk failed NACK
                return false;
            }
        }
    }

    // Hekper for evicting a failed bank, simply remove it from the group
    private void evictFailedServer(String bankName){
        group.leave(bankName);
        System.out.println("[MDS] Evicted: " + bankName + " from group " + group.getGroupName());
    }

    // Broadcasts the passed transactions to all the group members of the coordinator
    public void broadCastTransactions(List<Transaction> orderedTransactions)  {
        // Early return if there are no members
        if(group.getMembers().isEmpty()){
            return;
        }

        // Get the count and create a latch to synchronize broadcast
        int membersCount = group.getMembers().size();
        CountDownLatch latch = new CountDownLatch(membersCount);

        // Iterate over all banks
        for (BankServerInfo bankServerInfo : group.getMembers()) {
            BankService bank = bankServerInfo.getBank();

            // Submit jobs async to banks
            executor.submit(() -> {
                try {

                    // Try to send batch, return status
                    boolean success = sendWithRetry(bank, orderedTransactions);
                    if (!success) {
                        // If bank failed (timeout over 5s) evict bank
                        evictFailedServer(bankServerInfo.getName());
                    } else {
                        System.out.println("[MDS] ACK from bank ´" + bankServerInfo.getName() + "´");

                        // bank succeeded, get its snapshot, check if it exists and is changes, if it is changed
                        // update the coordinators snapshot
                        Pair tempSnapshot = bank.getBalance();
                        if (tempSnapshot != null && !tempSnapshot.equals(snapshot)) {
                            snapshot = tempSnapshot;
                        }
                    }
                }catch(RemoteException e){
                    e.printStackTrace();
                } 
                finally {
                    // Always count down, even if exceptions occur
                    latch.countDown();
                }
            });
        }
        try {
            // Await the latch before completing broadcast, ensure that all banks have received broadcast or failed
            latch.await();    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simply sets the transaction order
    public List<Transaction> setTransactionOrder(List<Transaction> transactions) {

        // Iterate over transactions to order, could be more efficient with a map
        for(Transaction tx: transactions){
            
            boolean alreadyIn = false;

            // Check if transaction is already in view, if it is, set flag and break loop
            for(Transaction viewTx: transactionView){

                if(tx.uniqueId().equals(viewTx.uniqueId())){
                    alreadyIn = true;
                    break;
                }
            }
            // dont add to view if the transaction already exists
            if(alreadyIn){
                continue;
            }

            // Add the transaction to the view
            transactionView.add(tx);
        }

        return transactionView; // Return the complete view after appending the transactions
    }

    // Simply removes the getSyncedBalance from the view (snapshot) by iterating over the view
    public void removeGetSyncedBalance() {
    for (int i = transactionView.size() - 1; i >= 0; i--) {
        String command = transactionView.get(i).command().split(" ")[0];
        if (command.equalsIgnoreCase("getsyncedbalance")) {
            transactionView.remove(i);
        }
    }
}

    // For getting the group of the coordinator
    public Group getGroup() {
        return group;
    }

    // For getting the view of the coordinator
    public List<Transaction> getTransactionView(){
        return transactionView;
    }
}
