import java.util.Collection;
import java.util.Map;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;


public class BankRepository{

    private final Map<String, CurrencyInfo> currencies;
    private final Collection<Transaction> outstanding_collections;

    //For history and metods 5-7
    private final List<ExecutedEntry> executedList = new CopyOnWriteArrayList<>();
    private final AtomicLong orderCounter = new AtomicLong(0);

     private static final class ExecutedEntry {
        final long orderNo;
        final Instant executedAt;
        final Transaction tx;
        ExecutedEntry(long orderNo, Instant executedAt, Transaction tx) {
            this.orderNo = orderNo; this.executedAt = executedAt; this.tx = tx;
        }
    }


    public BankRepository(Map<String, CurrencyInfo> currencies, Collection<Transaction> outstanding_collections){
        this.currencies = currencies;
        this.outstanding_collections = outstanding_collections;
    }

    public void getQuickBalance(String currency) {
        double totalBalance = currencies.get(currency).getAccountValue();

        for(String currString: currencies.keySet()){
            if(!currString.equals(currency)){
                double amountPresent = currencies.get(currString).getAccountValue();
                double amountPresentInDollar = amountPresent * currencies.get(currString).getRate();
                totalBalance += amountPresentInDollar / currencies.get(currency).getRate();
            }
        }

        System.out.println("Balance for " + currency + " is " + totalBalance);
    }
    
    public void deposit(String currency, double amount) {
        currencies.get(currency).add(amount);
    }
    public void addInterest(String currency, double percent) {
        currencies.get(currency).addInterest(percent/100f);
    }

    public void getSyncedBalanceNaive(String currency) {
        Thread t = new Thread(() -> {
            synchronized (outstanding_collections) {
                try 
                {
                    while (!outstanding_collections.isEmpty()) {
                        outstanding_collections.wait();
                    }
                    
                    getQuickBalance(currency);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        t.start();
    }

    public void notifyGetSyncedBalance() {
        synchronized (outstanding_collections) {
            outstanding_collections.notifyAll();
        }
    }

     // (5) getHistory
    public String getHistory() {
        StringBuilder sb = new StringBuilder();
        long start = orderCounter.get() - executedList.size();

        sb.append("executed_list\n");
        for (int i = 0; i < executedList.size(); i++) {
            ExecutedEntry e = executedList.get(i);
            long n = start + i + 1; 
            sb.append(n).append(". ")
              .append(e.executedAt).append(" ")
              .append(e.tx.getCommand()).append("\n");
        }

        sb.append("\noutstanding_collection\n");
        for (Transaction t : outstanding_collections) {
            sb.append(t.getCommand()).append("\n");
        }
        return sb.toString();
    }

    // (6) checkTxStatus <unique_id>
    public String checkTxStatus(String uniqueId) {
        for (ExecutedEntry e : executedList) {
            if (e.tx.getUniqueId().equals(uniqueId)) return "APPLIED";
        }
        for (Transaction t : outstanding_collections) {
            if (t.getUniqueId().equals(uniqueId)) return "OUTSTANDING";
        }
        return "UNKNOWN";
    }

     // (7) cleanHistory – clean executed_list (not counter)
    public void cleanHistory() {
        executedList.clear();
    }

    // Helper for when the batch is sent
    public void recordExecuted(Transaction t) {
        long n = orderCounter.incrementAndGet();
        executedList.add(new ExecutedEntry(n, Instant.now(), t));
    }


}