import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MessageDeliveryServer {

    private static final String REGISTRY_IP = "localhost";
    private static final int REGISTRY_PORT = 1099;

    public static void main(String[] args) throws RemoteException {

        // Create the group
        Group group = new Group("group3");
        System.out.println("[MDS] Created group");

        // When starting server, locate the RMI registry
        Registry registry = LocateRegistry.getRegistry(REGISTRY_IP, REGISTRY_PORT);
        System.out.println("[MDS] Located registry");

        System.out.println("[MDS] Creating registry");
        TransactionCoordinator coordinator = new TransactionCoordinator(group);

        // Create the service for registration with the group
        MessageDeliveryServiceImpl service = new MessageDeliveryServiceImpl(coordinator);

        // bind service to registry
        System.out.println("[MDS] Creating service");
        registry.rebind("message-delivery-service", service);

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
