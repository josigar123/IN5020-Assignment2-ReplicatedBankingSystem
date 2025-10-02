import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionCoordinator {

    private final Group group; // Group to coordinate

    private final AtomicInteger orderCounter = new AtomicInteger(1);
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Transaction> transactionView = new CopyOnWriteArrayList<>();

    public TransactionCoordinator(Group group) {
        this.group = group;
    }

    private boolean sendWithRetry(BankService bank, List<Transaction> batch) {
        long start = System.currentTimeMillis();
        int attempt = 0;

        while(true){
            attempt++;
            Future<Boolean> future = executor.submit(() -> bank.deliverOrderedBatch(batch));
            try{
                boolean ack = future.get(2, TimeUnit.SECONDS);
                if(ack){
                    System.out.println("ACK from bank after " + attempt + " attempt(s)");
                    Map<String, Double> bankBalance = bank.getBalance();

                    if(bankBalance != group.getCurrentBalance()){
                        System.out.println("Setting current balance to " + bankBalance);
                        group.setCurrentBalance(bankBalance);
                    }

                    return true;
                }
            }catch(TimeoutException e){
                future.cancel(true);
                System.out.println("Timeout, retrying...");
            }catch (Exception e){
                System.out.println(e.getMessage());
            }

            if(System.currentTimeMillis() - start > 5000){
                System.out.println("Bank failed after 5s");
                return false;
            }
        }
    }

    private void evictFailedServer(String bankName){
        group.leave(bankName);
        System.out.println("Evicted: " + bankName);
    }

    // Method must broadcast received transactions to all group members
    public void broadCastTransactions(List<Transaction> orderedTransactions) {

        for(BankServerInfo bankServerInfo : group.getMembers()){

            BankService bank = bankServerInfo.getBank();

            executor.submit(() -> {
                boolean success = sendWithRetry(bank, orderedTransactions);
                if(!success){
                    evictFailedServer(bankServerInfo.getName());
                }
            });
        }
    }

    public List<Transaction> setTransactionOrder(List<Transaction> transactions) {
        // Add all transactions to order,
        // Order will be implicit by iterating incrementally over the list
        transactionView.addAll(transactions);
        return transactionView; // Return the complete view after appending the transactions
    }

    public Group getGroup() {
        return group;
    }
}
