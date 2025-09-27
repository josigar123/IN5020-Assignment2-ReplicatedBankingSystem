import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.OptionalInt;

public class BankServer {

    private static final String REGISTRY_IP = "localhost";
    private static final int REGISTRY_PORT = 1099;

    public static void main(String[] args) throws RemoteException {
        if (args.length < 3) {
            System.err.println("Usage: BankServer <groupName> <bankBindingName> <mdsBindingName>");
            System.err.println("Example: BankServer group3 bank-R1 message-delivery-service");
            System.exit(1);
        }

        String groupName       = args[0]; // e.g. "group3"
        String bankBindingName = args[1]; // e.g. "bank-R1"
        String mdsBindingName  = args[2]; // e.g. "message-delivery-service"

        // 1) Locate the RMI registry
        Registry registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
        System.out.println("[BANK] Located registry");

        try {
            // 2) Look up the MDS service
            MessageDeliveryService mds = (MessageDeliveryService) registry.lookup(mdsBindingName);
            System.out.println("[BANK] Found MDS: " + mdsBindingName);

            // 3) Create and bind BankService implementation (before joinGroup)
            BankServiceImpl bankImpl = new BankServiceImpl(groupName);
            registry.rebind(bankBindingName, bankImpl);
            System.out.println("[BANK] Bound BankService as: " + bankBindingName);

            // 4) Join the group on MDS (MDS will look up this bank stub using bankBindingName)
            OptionalInt maybeBalance = mds.joinGroup(groupName, bankBindingName);
            maybeBalance.ifPresent(value -> {
                System.out.println("[BANK] Initial balance from MDS: " + value);
                bankImpl.setInitialBalance(value, "USD"); // change currency if you use another
            });

            System.out.println("[BANK] Bank server started");

            // 5) Keep the process alive
            synchronized (BankServer.class) {
                try { BankServer.class.wait(); } catch (InterruptedException ignored) {}
            }

        } catch (NotBoundException e) {
            System.err.println("[BANK] Could not find MDS binding: " + mdsBindingName);
            e.printStackTrace();
            System.exit(2);
        }
    }
}
