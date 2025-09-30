import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BankServer {

    private static final String REGISTRY_IP = "localhost";
    private static final int REGISTRY_PORT = 1099;

    // For 8-10 methods
    private static volatile List<String> CURRENT_MEMBERS = List.of();
    public static void setCurrentMembers(List<String> members) {
        CURRENT_MEMBERS = List.copyOf(members);
    }

    public static void main(String[] args) throws RemoteException {
        if (args.length < 5) {
            System.err.println("Usage: BankServer <bankBindingName> <mdsBindingName> <accountName> <numberOfReplicas> <currencyFileName> [OPTIONAL] <transactionFileName>");
            System.err.println("Example: BankServer bank-R1 message-delivery-service group3 3 TradingRate.txt [OPTIONAL] transactions.txt");
            System.exit(1);
        }

        // Collect all relevant arguments, the must always be present
        String bankBindingName = args[0]; // e.g. "bank-R1"
        String mdsBindingName = args[1]; // e.g. "message-delivery-service"
        String accountName = args[2]; // e.g. "group3"
        int numberOfReplicas = Integer.parseInt(args[3]); // e.g. 3
        String currencyFileName = args[4]; // e.g. "path/to/currencies.txt"
        String transactionFileName = null;

        if (args.length == 6) {
            transactionFileName = args[5]; // e.g. (optional) "path/to/transactions.txt"
        }else if (args.length > 6){
            System.err.println("Illegal number of arguments: " +  args.length);
            System.exit(2);
        }

        // Create a scanner, only if a transaction file is not supplied
        Scanner scanner = transactionFileName == null ? new Scanner(System.in) : null;

        // 1) Locate the RMI registry
        Registry registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
        System.out.println("[BANK] Located registry");

        try {
            // 2) Look up the MDS service
            MessageDeliveryService mds = (MessageDeliveryService) registry.lookup(mdsBindingName);
            System.out.println("[BANK] Found MDS: " + mdsBindingName);

            // 3) Create and bind BankService implementation (before joinGroup)
            BankServiceImpl bankImpl = new BankServiceImpl(accountName);
            registry.rebind(bankBindingName, bankImpl);
            System.out.println("[BANK] Bound BankService as: " + bankBindingName);

            // 4) Join the group on MDS (MDS will look up this bank stub using bankBindingName)
            Map<String, Double> maybeBalance = mds.joinGroup(accountName, bankBindingName);

            if(!maybeBalance.isEmpty()){
                System.out.println("[BANK] Bank joined group, setting initial balances...");
                bankImpl.setInitialBalance(maybeBalance);
            }

            System.out.println("[BANK] Waiting for all banks to join group...");
            mds.awaitExecution();
            System.out.println("[BANK] Bank server started");

            CommandParser parser = new CommandParser(bankImpl.getRepository());

            if(scanner != null){
                System.out.println("[BANK] Interactive CLI started...");
                while(true){
                    System.out.printf("[BANK] (%s) > ", accountName);
                    String input = scanner.nextLine().trim(); // Parse this line Stavros!
                    String result = parser.parseAndExecute(input);
                    System.out.println(result);

                    if (input.equalsIgnoreCase("exit")) {
                        exitProcess();
                        break;
                    }
                }
                
            }else{
                // In this case we are in batch mode
                System.out.println("[BANK] Processing transaction batch...");
                try{
                    // Read all transactions from supplied file
                    List<String> transactions = readAllLines(new File(transactionFileName));
                    for (String line : transactions) {
                        String result = parser.parseAndExecute(line);
                        System.out.println(result);
                    }


                }
                catch(IOException e){
                    e.printStackTrace();
                    exitProcess();
                }

                // 5) Keep the process alive
                synchronized (BankServer.class) {
                    try { BankServer.class.wait(); } catch (InterruptedException ignored) {}
                }
            }


        } catch (NotBoundException e) {
            System.err.println("[BANK] Could not find MDS binding: " + mdsBindingName);
            e.printStackTrace();
            System.exit(2);
        }
    }

    private static List<String> readAllLines(File f) throws IOException { 
        List<String> out;
        out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) { 
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) out.add(line); 
            }
        }
        return out;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private static Map<String, CurrencyInfo> initializeCurrency(String pathToRateFile){ 
        List<String> lines;
        try {
            lines = readAllLines(new File(pathToRateFile));   
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


    // (8) memberInfo
    public static String memberInfo() {
        return CURRENT_MEMBERS.isEmpty() ? "(no members)" : String.join("\n", CURRENT_MEMBERS);
    }

    // (9) sleep <seconds>
    public static String sleepSeconds(double seconds) {
        long ms = Math.round(seconds * 1000.0);
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        return "OK";
    }


    // (10) exit
    public static void exitProcess() {
        System.out.println("[BANK] Exiting...");
        System.exit(0);
    }


}
