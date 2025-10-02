import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BankServiceImpl extends UnicastRemoteObject implements BankService {

    private final String groupName;
    private final BankRepository repository;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BankServiceImpl(String groupName, BankRepository repository) throws RemoteException {
        this.groupName = groupName;
        this.repository = repository;
    }

    @Override
    public boolean deliverOrderedBatch(List<Transaction> orderedTransactions) throws RemoteException {

        // Add the received view from the MDS
        for(Transaction transaction : orderedTransactions){
            repository.addOutstandingTransaction(transaction);
        }

        executor.submit(()->{
           int selfCounter = 0;
           CommandParser parser = new CommandParser(repository);
           for(Transaction transaction : repository.getOutstandingTransactions()){
               boolean skipExecution = false;
               String prefix = transaction.getUniqueId().split(":")[0];
               // Filter stale transactions
               for(Transaction executedTransaction : repository.getExecutedTransactions()){
                   if(executedTransaction.getUniqueId().equals(transaction.getUniqueId())){
                       repository.getOutstandingTransactions().remove(executedTransaction);
                       skipExecution = true;
                   }
               }

               if(skipExecution){
                   continue;
               }

               if(repository.getBankBindingName().equals(prefix) && selfCounter == repository.getSelfTransactions()){
                   continue;
               }

               if(repository.getBankBindingName().equals(prefix) && selfCounter < repository.getSelfTransactions()){
                  selfCounter++;
               }

               // This method appends the transaction to the executed list, and also remove from outstanding collections
               parser.executeTransaction(transaction.command());

           }
        });

        return true; // ACK
    }

    @Override
    public void onMembershipChange(String group, List<String> currentReplicaIds) throws RemoteException {
        throw  new UnsupportedOperationException("Not supported yet.");
    }

    // TODO: Implement this so that new banks can get the correct view
    //       MDS iterates over all group members, fetching their balance until the first correct return
    @Override
    public Map<String, Double> getBalance() throws RemoteException {
        throw  new UnsupportedOperationException("Not supported yet.");
    }


    public BankRepository getRepository() {
        return this.repository;
}

}



