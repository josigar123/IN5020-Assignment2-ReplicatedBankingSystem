import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class BankServiceImpl extends UnicastRemoteObject implements BankService {

    // Implementation of the BankService interface
    // This class receives ordered transaction batches from the MDS, and applies them to the local BankRepository
    // It ensures sequential consistency using a lock and a single-threaded executor

    private final BankRepository repository; // Local data store for this bank
    private final ReentrantLock lock = new ReentrantLock(true); // Ensures ordered and exclusive updates
    private Pair deliverPair = null;  // Latest snapshot of balance state

    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Handles async execution

    public BankServiceImpl(BankRepository repository) throws RemoteException {
        this.repository = repository;
    }

    // Called by MDS when it delivers an ordered batch of transactions
    // Each transaction is applied sequentially to the local repository
    // The method is executed asynchronously but protected by a lock to guarantee consistency within this replica

    @Override
    public boolean deliverOrderedBatch(List<Transaction> orderedTransactions) throws RemoteException {
        executor.submit(()->{
            lock.lock();
            try {
                deliverPair = null; // Reset state before applying new batch
                int idx = repository.getOrderCounter().intValue();
                CommandParser parser = new CommandParser(repository);
                // Apply all transactions in the given order
                for(int i = idx; i < orderedTransactions.size(); i++){
                    String command = parser.parseTransactionFromLine(orderedTransactions.get(i).command())[0];
                    String bankName = orderedTransactions.get(i).uniqueId().split(":")[0];
                    Transaction t = orderedTransactions.get(i);
                    // Execute getSyncedBalance only on the requesting bank
                    if(command.toLowerCase().equals("getsyncedbalance")){
                        if(bankName.equals(repository.getBankBindingName())){
                            parser.executeTransaction(t.command());
                        }
                    }
                    else{
                        // Apply normal transaction to this bank
                        parser.executeTransaction(t.command());

                        repository.addToExecutedList(t);
                        repository.getOrderCounter().incrementAndGet();
                    }
                    // Remove transaction from outstanding list once done
                    repository.getOutstandingTransactions().remove(t);
                    //repository.notifyGetSyncedBalance();
                } 
                // After all transactions are processed, create a new state snapshot
                deliverPair = new Pair(repository.getCurrencies(), repository.getOrderCounter().intValue());     
            } finally {
                lock.unlock();
            }
        });

        return true; // ACK
    }

    // Called by MDS to retrieve the latest state snapshot from this bank
    // The method waits until the previous batch has been fully applied, 
    // and then returns the resulting balance and order counter.

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public Pair getBalance() throws RemoteException {
        CompletableFuture<Pair> future = new CompletableFuture<>();
        executor.submit(()->{
            while(deliverPair == null){ 
            }
            future.complete(deliverPair);
        });
        try {
            return future.get();   // Return the latest snapshot
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Returns this bank’s repository 
    public BankRepository getRepository() {
        return this.repository;
    }

    // Shut down the single-thread executor
    public void exitExecutor(){
        executor.shutdown();
    }

}