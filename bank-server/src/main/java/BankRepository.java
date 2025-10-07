import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class BankRepository{

    private Map<String, CurrencyInfo> currencies;
    private final List<Transaction> outstandingCollections = new CopyOnWriteArrayList<>();

    private final Map<Transaction, LocalTime> transactionTimeMap = new HashMap<>();
    private LocalTime executionTimestamp;
    private final String accountName;
    private final List<Transaction> executedList = new CopyOnWriteArrayList<>();
    private AtomicLong orderCounter = new AtomicLong(0);
    private final AtomicLong outstandingCounter = new AtomicLong(0);

    private final MessageDeliveryService messageDeliveryService; // This holds the stub to the MDS
    private final String bankBindingName;

    public BankRepository(String accountName, String bankBindingName, MessageDeliveryService messageDeliveryService, String pathToCurrencyFile){
        this.bankBindingName = bankBindingName;
        this.currencies = initializeCurrency(pathToCurrencyFile);
        this.messageDeliveryService = messageDeliveryService;
        this.accountName = accountName;
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

        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") getQuickBalance " + currency + ". Total Balance: " + totalBalance);
    }

    private void log(String message) {
        // Directory to store logs
        String dir = "results";
        File directory = new File(dir);
        if (!directory.exists()) {
            directory.mkdirs(); // create directory if it doesn't exist
        }

        // File path
        String fileName = dir + "/" + bankBindingName + "Results.txt";

        try (PrintWriter out = new PrintWriter(new FileWriter(fileName, true))) { // append mode
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    public void deposit(String currency, double amount) {
        currencies.get(currency).add(amount);
        LocalTime now = LocalTime.now();
        executionTimestamp = now;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") deposit " + currency + " " + amount);
    }
    public void addInterest(String currency, double percent) {
        LocalTime now = LocalTime.now();
        executionTimestamp = now;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        if (currency == null) {
            
            for (Map.Entry<String, CurrencyInfo> entry : currencies.entrySet()) {
                entry.getValue().addInterest(percent / 100f);
            }
            log("(" + now.format(formatter) + ") addInterest all currencies " + percent + "%");
        } else {
            currencies.get(currency).addInterest(percent / 100f);
            log("(" + now.format(formatter) + ") addInterest " + currency + " " + percent + "%");
        }
    }

    public void getSyncedBalanceSmart(String currency) {
        double totalBalance = currencies.get(currency).getAccountValue();

        for(String currString: currencies.keySet()){
            if(!currString.equals(currency)){
                double amountPresent = currencies.get(currString).getAccountValue();
                double amountPresentInDollar = amountPresent * currencies.get(currString).getRate();
                totalBalance += amountPresentInDollar / currencies.get(currency).getRate();
            }
        }

        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") getSyncedBalanceSmart " + currency + ". Total Balance: " + totalBalance);
    }

    public void getSyncedBalanceNaive(String currency) {
        Thread t = new Thread(() -> {
            synchronized (outstandingCollections) {
                try 
                {
                    while (!outstandingCollections.isEmpty()) {
                        outstandingCollections.wait();
                    }
                    
                    double totalBalance = currencies.get(currency).getAccountValue();

                    for(String currString: currencies.keySet()){
                        if(!currString.equals(currency)){
                            double amountPresent = currencies.get(currString).getAccountValue();
                            double amountPresentInDollar = amountPresent * currencies.get(currString).getRate();
                            totalBalance += amountPresentInDollar / currencies.get(currency).getRate();
                        }
                    }

                    LocalTime now = LocalTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    log("(" + now.format(formatter) + ") getSyncedBalanceNaive " + currency + ". Total Balance: " + totalBalance);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        t.start();
    }

    public void notifyGetSyncedBalance() {
        synchronized (outstandingCollections) {
            outstandingCollections.notifyAll();
        }
    }

    public void memberInfo(String groupName) throws RemoteException{
        System.out.println("[BANK] Getting members for group " + groupName + ":");
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") memberInfo " + groupName + ":");
        for(String member : messageDeliveryService.getMemberNames(groupName)){
            log("\t"+member);
            System.out.println(member);
        }
    }

     // (5) getHistory
    public void getHistory() {
        StringBuilder sb = new StringBuilder();
        long start = orderCounter.get() - executedList.size();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        sb.append("[BANK] Transaction history:\n");

        sb.append("\tExecuted transactions:\n");
        for (int i = 0; i < executedList.size(); i++) {
            Transaction t = executedList.get(i);
            long n = start + i + 1; 
            sb.append("\t\t").append(n).append(". ")
              .append(transactionTimeMap.get(t).format(formatter)).append(" ")
              .append(t.getCommand()).append(" ")
              .append(t.getUniqueId()).append("\n");
        }

        sb.append("\n\tOutstanding transactions:\n");
        for (int i = 0; i < outstandingCollections.size(); i++) {
            Transaction t = outstandingCollections.get(i);
            long n = start + i + 1;
            sb.append("\t\t").append(n).append(". ").append(t.getCommand()).append(" ")
                    .append(t.getUniqueId()).append("\n");
        }
        LocalTime now = LocalTime.now();
        log("(" + now.format(formatter) + ") getHistory:\n " + sb);
        System.out.println(sb);
    }

    // (6) checkTxStatus <unique_id>
    public void checkTxStatus(String uniqueId) {
        for (Transaction t : outstandingCollections) {
            System.out.print(t.getUniqueId());
            String[] extractedTValue = t.getUniqueId().split(":");
            if (extractedTValue[1].equals(uniqueId)) {
                LocalTime now = LocalTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                log("(" + now.format(formatter) + ") checkTxStatus  " + uniqueId + ": OUTSTANDING");
                System.out.println("[BANK] TRANSACTION  ´" + uniqueId + "´ IS OUTSTANDING");
                return;
            }
        }

        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") checkTxStatus  " + uniqueId + ": APPLIED");
        System.out.println("[BANK] TRANSACTION ´" + uniqueId + "´ IS APPLIED");
    }

     // (7) cleanHistory – clean executed_list (not counter)
    public void cleanHistory() {
        executedList.clear();
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") cleanHistory");
    }

    public void sleep(double seconds) throws InterruptedException {
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") sleep " + seconds + "s");
        long ms = Math.round(seconds * 1000.0);
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    public void exit(boolean isInteractive) throws RemoteException {
        System.out.println("[BANK] Exiting...");

        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (Map.Entry<String, CurrencyInfo> entry : currencies.entrySet()) {
            log("currency: " + entry.getKey() + " -> " + entry.getValue().getAccountValue());
        }
        log("(" + now.format(formatter) + ") exit");
        messageDeliveryService.leaveGroup(accountName, bankBindingName);
        if(!isInteractive){
            System.exit(0);
        }
    }

    public void addOutstandingTransaction(Transaction transaction){
        outstandingCollections.add(transaction);
        outstandingCounter.incrementAndGet();
    }


    @SuppressWarnings("CallToPrintStackTrace")
    private Map<String, CurrencyInfo> initializeCurrency(String pathToRateFile){
        List<String> lines;
        try {
            lines = Utils.readAllLines(new File(pathToRateFile));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Map<String,CurrencyInfo> retValue = new HashMap<>();

        for(String line: lines){
            String[] parts = line.split("\\s+");
            CurrencyInfo newCurrency = new CurrencyInfo(parts[0], Double.parseDouble(parts[1]));
            retValue.put(parts[0], newCurrency);
        }

        retValue.put("USD", new CurrencyInfo("USD", 1f));

        return retValue;
    }

    public String getBankBindingName() {
         return bankBindingName;
    }

    public AtomicLong getOutstandingCount() {
         return outstandingCounter;
    }

    public List<Transaction> getOutstandingTransactions() {
         return this.outstandingCollections;
    }

    public List<Transaction> getExecutedTransactions() {
        return executedList;
    }

    public void addToExecutedList(Transaction tAdd){
        executedList.add(tAdd);
        transactionTimeMap.put(tAdd, executionTimestamp);
    }

    public AtomicLong getOrderCounter(){
        return orderCounter;
    }

    public Map<String, CurrencyInfo> getCurrencies(){
        return currencies;
    }

    public void setState(Pair snapshot){
        this.orderCounter = new AtomicLong(snapshot.getOrderCounter());
        this.currencies = snapshot.getCurrencies();
    }

    public String getAccountName() {
        return accountName;
    }
}