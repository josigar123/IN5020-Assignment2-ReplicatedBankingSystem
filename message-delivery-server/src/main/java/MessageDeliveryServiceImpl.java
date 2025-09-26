import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public class MessageDeliveryServiceImpl implements MessageDeliveryService{


    @Override
    public void joinGroup(String groupName, String bankServer) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'joinGroup'");
    }

    @Override
    public void leaveGroup(String groupName, String bankServer) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'leaveGroup'");
    }

    @Override
    public void sendTransactions(List<Transaction> transactions) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendTransactions'");
    }
    
}
