import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ConcurrentHashMap;

public class MessageDeliveryServer {

    // Assumed that registry is on localhost on 1099, this must be changed if you launch the registry with other
    // parameters
    private static final String REGISTRY_IP = "localhost";
    private static final int REGISTRY_PORT = 1099;

    public static void main(String[] args) throws RemoteException {

        // Create the group
        Group group = new Group("group3");
        System.out.println("[MDS] Created group");

        // When starting server, locate the RMI registry
        Registry registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
        System.out.println("[MDS] Located registry");

        // Create a coordinator for the group
        System.out.println("[MDS] Creating coordinator");
        TransactionCoordinator coordinator = new TransactionCoordinator(group);

        // Concurrent hashmap of coordinators to pass to MDSImpl, this is so that multiple groups can be created
        // (out of scope for this assignment)
        ConcurrentHashMap<String, TransactionCoordinator> coordinators = new ConcurrentHashMap<>();

        // Add the one group to the map (can add more)
        coordinators.put(coordinator.getGroup().getGroupName(), coordinator);

        // Pass the map of coordinators, the MDSImpl, can now coordinate for multiple groups, each with their
        // own coordinator
        MessageDeliveryServiceImpl service = new MessageDeliveryServiceImpl(coordinators);

        // bind service to registry
        System.out.println("[MDS] Creating service");
        registry.rebind("message-delivery-service", service);
        System.out.println("[MDS] Bound service");

        System.out.println("[MDS] Message delivery server started");

        // Holds the thread
        synchronized (MessageDeliveryServer.class) {
            try {
                MessageDeliveryServer.class.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }
}
