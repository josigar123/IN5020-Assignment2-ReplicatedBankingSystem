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

public class TransactionCoordinator {

    private final Group group; // Group to coordinate

    private final AtomicInteger orderCounter = new AtomicInteger(0);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Transaction> transactionView = new CopyOnWriteArrayList<>();
    private Pair snapshot = null;

    public TransactionCoordinator(Group group) {
        this.group = group;
    }

    private boolean sendWithRetry(BankService bank, List<Transaction> batch) {
        long start = System.currentTimeMillis();

        while(true){
            Future<Boolean> future = executor.submit(() -> bank.deliverOrderedBatch(batch));
            try{
                boolean ack = future.get(2, TimeUnit.SECONDS);
                if(ack){
                    snapshot = bank.getBalance();
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
                return false;
            }
        }
    }

    private void evictFailedServer(String bankName){
        group.leave(bankName);
        System.out.println("[MDS] Evicted: " + bankName + " from group " + group.getGroupName());
    }

    // Method must broadcast received transactions to all group members
    public void broadCastTransactions(List<Transaction> orderedTransactions)  {
        if(group.getMembers().isEmpty()){
            return;
        }

        int membersCount = group.getMembers().size();
        CountDownLatch latch = new CountDownLatch(membersCount);

        for (BankServerInfo bankServerInfo : group.getMembers()) {
            BankService bank = bankServerInfo.getBank();
            executor.submit(() -> {
                try {

                    boolean success = sendWithRetry(bank, orderedTransactions);
                    if (!success) {
                        evictFailedServer(bankServerInfo.getName());
                    } else {
                        System.out.println("[MDS] ACK from bank ´" + bankServerInfo.getName() + "´");
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
            latch.await();    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Transaction> setTransactionOrder(List<Transaction> transactions) {
        
        for(Transaction tx: transactions){
            
            boolean alreadyIn = false;
            
            for(Transaction viewTx: transactionView){

                if(tx.uniqueId().equals(viewTx.uniqueId())){
                    alreadyIn = true;
                    break;
                }
            }
            if(alreadyIn){
                continue;
            }
            transactionView.add(tx);
        }
        return transactionView; // Return the complete view after appending the transactions
    }

    public Group getGroup() {
        return group;
    }
}
