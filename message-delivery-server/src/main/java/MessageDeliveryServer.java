import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;

public class MessageDeliveryServer {

    private static final String REGISTRY_IP = "localhost";
    private static final int REGISTRY_PORT = 1099;

    public static void main(String[] args) throws RemoteException {

        int initialNumberOfReplicas = Integer.parseInt(args[0]);
        // Create the group
        Group group = new Group("group3");
        System.out.println("[MDS] Created group");

        // When starting server, locate the RMI registry
        Registry registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
        System.out.println("[MDS] Located registry");

        System.out.println("[MDS] Creating coordinator");
        TransactionCoordinator coordinator = new TransactionCoordinator(group);
        // Concurrent hashmap of coordinators to pass to MDSImpl
        ConcurrentHashMap<String, TransactionCoordinator> coordinators = new ConcurrentHashMap<>();
        // Add the one group to the map
        coordinators.put(coordinator.getGroup().getGroupName(), coordinator);

        // Pass the map of coordinators, the MDSImpl, can now coordinate for multiple groups, each with their
        // own coordinator
        MessageDeliveryServiceImpl service = new MessageDeliveryServiceImpl(coordinators, initialNumberOfReplicas);

        // bind service to registry
        System.out.println("[MDS] Creating service");
        registry.rebind("message-delivery-service", service);
        System.out.println("[MDS] Bound service");

        System.out.println("[MDS] Message delivery server started");

        synchronized (MessageDeliveryServer.class) {
            try {
                MessageDeliveryServer.class.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }
}
