import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BankServer {

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final String REGISTRY_IP = "localhost";
    private static final int REGISTRY_PORT = 1099;

    private final String bankBindingName; // e.g. "bank-R1"
    private final String mdsBindingName; // e.g. "message-delivery-service"
    private final String accountName; // e.g. "group3"
    private final int numberOfReplicas; // e.g. 3
    private final String currencyFileName; // e.g. "path/to/currencies.txt"
    private final String transactionFileName;

    private final CommandParser parser;
    private final BankRepository repository;

   /*
    TODO:
        2. Refaktorer parser til å kalle alle metodene i BankRepository OK
        14. CommandParser må legge til transaksjoner i outstanding_collections OK
        15. Etter du har sent Outstanding_collections og fått view fra mds, må commandparser kjøre kommandoene OK
    */

    public BankServer(BankConfig config) throws RemoteException, NotBoundException {
        bankBindingName = config.bankBindingName(); // e.g. "bank-R1"
        mdsBindingName = config.mdsBindingName(); // e.g. "message-delivery-service"
        accountName = config.accountName(); // e.g. "group3"
        numberOfReplicas = config.numberOfReplicas(); // e.g. 3
        currencyFileName = config.currencyFileName(); // e.g. "path/to/currencies.txt"
        transactionFileName = config.transactionFileName(); // e.g. "path/to/transactions.txt"

        Registry registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
        System.out.println("[BANK] Located registry");

        MessageDeliveryService mds = (MessageDeliveryService) registry.lookup(mdsBindingName);
        System.out.println("[BANK] Found MDS: " + mdsBindingName);

        repository = new BankRepository(bankBindingName, mds, currencyFileName);
        System.out.println("[BANK] Created Bank Repository");

        BankServiceImpl bankImpl = new BankServiceImpl(repository);
        registry.rebind(bankBindingName, bankImpl);
        System.out.println("[BANK] Bound BankService as: " + bankBindingName);

        // Returns a maybeBalance, needs refactor
        Pair snapshot = mds.joinGroup(accountName, bankBindingName);
        if(snapshot != null){
            repository.setState(snapshot);
        }
        System.out.printf("[BANK] Joined group %s", accountName);

        System.out.println("[BANK] Waiting for all banks to join group...");
        mds.awaitExecution();
        System.out.println("[BANK] Bank server started");

        parser = new CommandParser(repository);
        System.out.println("[BANK] Command parser created");

        // Create a scheduler
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("[BANK] Sending outstanding collection to MDS...");
                mds.sendTransactions(accountName, repository.getOutstandingTransactions());
                repository.setSelfTransactions(repository.getOutstandingTransactions().size());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws RemoteException, NotBoundException {

        BankConfig bankConfig = BankConfig.fromArgs(args);
        BankServer bankServer = new BankServer(bankConfig);

        if(bankServer.isInteractive()){
            bankServer.launchInteractiveCli();
        }else{
            bankServer.launchBatchProcessing();
        }
}

    public boolean isInteractive(){
        return transactionFileName == null;
    }

    public void launchInteractiveCli(){
        System.out.println("[BANK] Interactive CLI started...");
        while(true){
            try(Scanner sc = new Scanner(System.in)){
                System.out.printf("[BANK] (%s) > ", accountName);
                String input = sc.nextLine().trim();

                // Will build the outstanding transactions collection for every read line
                parser.buildOutstandingTransactions(input);
            }
        }
    }

    public void launchBatchProcessing(){
        System.out.println("[BANK] Processing transaction batch...");
        try{
            // Read all transactions from supplied file
            List<String> transactions = Utils.readAllLines(new File(transactionFileName));
            for (String line : transactions) {
                Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1501));
                // Will build the outstanding transactions collection for every read line
                parser.buildOutstandingTransactions(line);
            }
        }
        catch(IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            repository.exit();
        }
    }

}
