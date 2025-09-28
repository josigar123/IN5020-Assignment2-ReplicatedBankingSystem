
import java.util.Collection;
import java.util.Map;


public class BankRepository{
    public class Transaction{};

    private final Map<String, CurrencyInfo> currencies;
    private final Collection<Transaction> outstanding_collections;

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
}