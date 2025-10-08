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

    // Map for storing the information about every currency (Account value, name, rateToUsd)
    private Map<String, CurrencyInfo> currencies;

    // Where transactions to be sent are stored
    private final List<Transaction> outstandingCollections = new CopyOnWriteArrayList<>();

    // Executed transactions and their timestamps when executed
    private final Map<Transaction, LocalTime> transactionTimeMap = new HashMap<>();
    private LocalTime executionTimestamp;

    // Name of the account for example group3
    private final String accountName;

    // Where executed transaction go
    private final List<Transaction> executedList = new CopyOnWriteArrayList<>();

    // How many orders have been executed by every bank thus far
    private AtomicLong orderCounter = new AtomicLong(0);

    // How many orders this bank has added to outstandingCollections
    private final AtomicLong outstandingCounter = new AtomicLong(0);

    // This holds the stub to the MDS
    private final MessageDeliveryService messageDeliveryService;

    // This holds the name of the bank. Used for setting transaction id
    private final String bankBindingName;

    // constructor
    public BankRepository(String accountName, String bankBindingName, MessageDeliveryService messageDeliveryService, String pathToCurrencyFile){
        this.bankBindingName = bankBindingName;
        this.currencies = initializeCurrency(pathToCurrencyFile);
        this.messageDeliveryService = messageDeliveryService;
        this.accountName = accountName;
    }

    // Checks what <currency> is by adding every other currency to it
    // Defaults to dollar if currency is null
    // Runs when called
    public void getQuickBalance(String currency) {

        currency = (currency != null) ? currency : "USD";

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

    // For outputting the results of bank execution
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

    // Deposits <amount> into <currency>
    public void deposit(String currency, double amount) {
        currencies.get(currency).add(amount);
        LocalTime now = LocalTime.now();
        executionTimestamp = now;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") deposit " + currency + " " + amount);
    }

    // adds a <percent> to <currency> or all currencies if <currency> is null
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

    // Checks what <currency> is by adding every other currency to it
    // Defaults to dollar if currency is null
    // Runs when it gets broadcasted back after being sent as a part of the view to mds
    public void getSyncedBalanceSmart(String currency) {

        currency = (currency != null) ? currency : "USD";

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

    // Checks what <currency> is by adding every other currency to it
    // Defaults to dollar if currency is null
    // Runs when it gets notified and outstandingCollections is empty
    public void getSyncedBalanceNaive(String currency) {

        currency = (currency != null) ? currency : "USD";
        final String finalCurrency = currency;

        Thread t = new Thread(() -> {
            synchronized (outstandingCollections) {
                try 
                {
                    while (!outstandingCollections.isEmpty()) {
                        outstandingCollections.wait();
                    }
                    
                    double totalBalance = currencies.get(finalCurrency).getAccountValue();

                    for(String currString: currencies.keySet()){
                        if(!currString.equals(finalCurrency)){
                            double amountPresent = currencies.get(currString).getAccountValue();
                            double amountPresentInDollar = amountPresent * currencies.get(currString).getRate();
                            totalBalance += amountPresentInDollar / currencies.get(finalCurrency).getRate();
                        }
                    }

                    LocalTime now = LocalTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    log("(" + now.format(formatter) + ") getSyncedBalanceNaive " + finalCurrency + ". Total Balance: " + totalBalance);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        t.start();
    }

    // Notifies getSyncedBalanceNaive thread
    public void notifyGetSyncedBalance() {
        synchronized (outstandingCollections) {
            outstandingCollections.notifyAll();
        }
    }


    // Asks MDS for information about current members with same account
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

    // Shows the current state of executedList
    // Shows the current state of outstandingCollections
    public void getHistory() {
        StringBuilder sb = new StringBuilder();
        long start = orderCounter.get() - executedList.size(); // Added logic for the numbering of each transaction
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

    // Checks if a transaction with <uniqeId> id is executed or not
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

     // clears executed_list (but doesnt reset orderCounter)
    public void cleanHistory() {
        executedList.clear();
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") cleanHistory");
    }

    // Sleeps the thread that calls it
    public void sleep(double seconds) throws InterruptedException {
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        log("(" + now.format(formatter) + ") sleep " + seconds + "s");
        long ms = Math.round(seconds * 1000.0);
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    // Exits the process thats being run after outputting the result of currencies
    // Calls System.exit for batch mode, but not for interactive mode.
    // This is cause Interactive mode is run on a thread in 1 jvm.
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

    // Adds to outstandingCollections
    public void addOutstandingTransaction(Transaction transaction){
        outstandingCollections.add(transaction);
        outstandingCounter.incrementAndGet();
    }

    // Adds to executedList and transactionTimeMap
    public void addToExecutedList(Transaction tAdd){
        executedList.add(tAdd);
        transactionTimeMap.put(tAdd, executionTimestamp);
    }

    // Setting the state for replicas that need to get synchronized with existing banks
    public void setState(Pair snapshot){
        this.orderCounter = new AtomicLong(snapshot.getOrderCounter());
        this.currencies = snapshot.getCurrencies();
    }


    // Reads a rate file to initialize the currencies all with 0 value at the start
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

    // Getters
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

    public AtomicLong getOrderCounter(){
        return orderCounter;
    }

    public Map<String, CurrencyInfo> getCurrencies(){
        return currencies;
    }

    public String getAccountName() {
        return accountName;
    }
}