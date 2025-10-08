import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BankServer {

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // For rmiregistry
    private static final String REGISTRY_IP = "localhost";
    private static final int REGISTRY_PORT = 1099;

    private final String bankBindingName; // e.g. "bank-R1"
    private final String mdsBindingName; // e.g. "message-delivery-service"
    private final String accountName; // e.g. "group3"
    private final int numberOfReplicas; // e.g. 3
    private final String currencyFileName; // e.g. "path/to/currencies.txt"
    private final String transactionFileName; // e.g. "Rep1.txt"

    private final CommandParser parser;
    private final BankRepository repository;

    // Constructor reads information from BankConfig
    public BankServer(BankConfig config) throws RemoteException, NotBoundException {
        bankBindingName = config.bankBindingName(); // e.g. "bank-R1"
        mdsBindingName = config.mdsBindingName(); // e.g. "message-delivery-service"
        accountName = config.accountName(); // e.g. "group3"
        numberOfReplicas = config.numberOfReplicas(); // e.g. 3
        currencyFileName = config.currencyFileName(); // e.g. "path/to/currencies.txt"
        transactionFileName = config.transactionFileName(); // e.g. "path/to/transactions.txt"

        // Find rmiregistry
        Registry registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
        System.out.println("[BANK] Located registry");

        // Find mds stub
        MessageDeliveryService mds = (MessageDeliveryService) registry.lookup(mdsBindingName);
        System.out.println("[BANK] Found MDS: " + mdsBindingName);

        // Create bank repository
        repository = new BankRepository(accountName, bankBindingName, mds, currencyFileName);
        System.out.println("[BANK] Created Bank Repository");

        // Create BankService skeleton for rmi
        BankServiceImpl bankImpl = new BankServiceImpl(repository);
        registry.rebind(bankBindingName, bankImpl);
        System.out.println("[BANK] Bound BankService as: " + bankBindingName);

        // Joins the group determined by accountname
        // Checks if existing replicas have run to share their state
        Pair snapshot = mds.joinGroup(accountName, bankBindingName, numberOfReplicas);
        if(snapshot != null){
            repository.setState(snapshot);
        }
        System.out.printf("[BANK] Joined group %s\n", accountName);

        System.out.println("[BANK] Waiting for all banks to join group " + accountName + "...");
        mds.awaitExecution();
        System.out.println("[BANK] Bank server started");

        parser = new CommandParser(repository);
        System.out.println("[BANK] Command parser created");

        // Create a scheduler for sending transactions every 10 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                //System.out.println("[BANK] Sending outstanding collection to MDS..."); // COMMENTED OUT, GOOD FOR DEUBG
                mds.sendTransactions(accountName, repository.getOutstandingTransactions());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws RemoteException, NotBoundException {

        // Runs interactive mode or batch mode depending on the user configuration
        BankConfig bankConfig = BankConfig.fromArgs(args);

        if(isInteractive(bankConfig)){
            List<BankServer> banks= instantiateBanks(bankConfig);
            launchInteractiveCli(banks);
        }else{
            BankServer bankServer = new BankServer(bankConfig);
            bankServer.launchBatchProcessing();
        }

        // explicit call for jvm that runs interactive threads
        System.exit(0);
    }

    // Starts BankServer threads for interactive mode. So one JVM will host numOfReplicas threads
    public static List<BankServer> instantiateBanks(BankConfig bankConfig) {
        ExecutorService executor = Executors.newFixedThreadPool(bankConfig.numberOfReplicas());
        List<BankServer> banks = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(bankConfig.numberOfReplicas());

        for(int i = 1; i <= bankConfig.numberOfReplicas(); i++){
            int id = i;
            executor.submit(() -> {
                try{
                    BankConfig config = new BankConfig(
                            bankConfig.bankBindingName() + id,
                            bankConfig.mdsBindingName(),
                            bankConfig.accountName(),
                            bankConfig.numberOfReplicas(),
                            bankConfig.currencyFileName(),
                            bankConfig.transactionFileName()
                    );
                    BankServer bankServer = new BankServer(config);
                    banks.add(bankServer);
                }catch(Exception e){
                    System.err.printf("[BANK-R%d] Failed to start: %s%n", id, e.getMessage());
                    e.printStackTrace();
                }finally {
                    latch.countDown();
                }
            });
        }

        try{
            latch.await();
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            System.err.println("[BANK] Interrupted while waiting for banks to start");
        }

        executor.shutdown();
        System.out.printf("[BANK] All %d banks initialized.%n", banks.size());

        return banks;
    }

    // Helper function for determining configuration
    public static boolean isInteractive(BankConfig bankConfig) {
        return bankConfig.transactionFileName() == null;
    }

    // Read reply loop for adding transactions through terminal
    public static void launchInteractiveCli(List<BankServer> servers){
        System.out.println("[BANK] Interactive CLI started. Type 'exit' to quit.'");
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.printf("[BANK] (%s) > ", servers.get(0).accountName);
            if (!sc.hasNextLine()) break;

            String input = sc.nextLine().trim();
            boolean exitFlag = false;
            if (input.isEmpty()) continue;
            for(BankServer bank : servers){
                if(input.equals("exit")){
                    exitFlag = true;
                }
                if(!exitFlag){
                    bank.parser.buildOutstandingTransactions(input);
                }
                else{
                    bank.parser.buildOutstandingTransactions("exitinteractive");
                }
            }
            if(exitFlag){
                break;
            }
        }
    }

    // Inserts transactions from file used in batch mode.
    public void launchBatchProcessing() throws RemoteException {
        System.out.println("[BANK] Processing transaction batch...");
        try{
            // Read all transactions from supplied file
            List<String> transactions = Utils.readAllLines(new File(transactionFileName));
            for (String line : transactions) {
                Thread.sleep(ThreadLocalRandom.current().nextLong(500, 1501)); // random value as described in assignment
                // Will build the outstanding transactions collection for every read line
                parser.buildOutstandingTransactions(line);
            }
        }
        catch(IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            repository.exit(false);
        }
    }

    // getter
    public BankRepository getRepository(){
        return this.repository;
    }

}
