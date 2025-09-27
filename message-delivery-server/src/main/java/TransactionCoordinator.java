import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionCoordinator {

    private final Group group; // Group to coordinate
    private final List<OrderedTransaction> orderedTransactions;
    private final AtomicInteger orderCounter;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TransactionCoordinator(Group group) {
        this.group = group;
        orderCounter = new AtomicInteger(1);
        orderedTransactions = Collections.synchronizedList(new ArrayList<>());
    }

    private boolean sendWithRetry(BankService bank, List<OrderedTransaction> batch) {
        long start = System.currentTimeMillis();
        int attempt = 0;

        while(true){
            attempt++;
            Future<Boolean> future = executor.submit(() -> bank.deliverOrderedBatch(batch));
            try{
                boolean ack = future.get(2, TimeUnit.SECONDS);
                if(ack){
                    System.out.println("ACK from bank after " + attempt + " attempt(s)");
                    int bankBalance = bank.getBalance();

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
    public void broadCastTransactions() {

        List<OrderedTransaction> batch;
        synchronized (orderedTransactions) {
            batch = new ArrayList<>(orderedTransactions);
        }

        for(BankServerInfo bankServerInfo : group.getMembers()){

            BankService bank = bankServerInfo.getBank();

            executor.submit(() -> {
                boolean success = sendWithRetry(bank, batch);
                if(!success){
                    evictFailedServer(bankServerInfo.getName());
                }
            });
        }
    }

    public void setTransactionOrder(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            orderedTransactions.add(new OrderedTransaction(orderCounter.getAndIncrement(), transaction));
        }
    }

    public Group getGroup() {
        return group;
    }
}
