import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class BankServiceImpl extends UnicastRemoteObject implements BankService {

    private final BankRepository repository;
    private final ReentrantLock lock = new ReentrantLock(true); 
    private Pair deliverPair = null;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BankServiceImpl(BankRepository repository) throws RemoteException {
        this.repository = repository;
    }

    @Override
    public boolean deliverOrderedBatch(List<Transaction> orderedTransactions) throws RemoteException {
        executor.submit(()->{
            lock.lock();
            try {
                deliverPair = null;
                int idx = repository.getOrderCounter().intValue();
                CommandParser parser = new CommandParser(repository);
                for(int i = idx; i < orderedTransactions.size(); i++){
                    parser.executeTransaction(orderedTransactions.get(i).command());
                    repository.getOrderCounter().incrementAndGet();
                }
                deliverPair = new Pair(repository.getCurrencies(), repository.getOrderCounter().intValue());     
            } finally {
                lock.unlock();
            }
        });

        return true; // ACK
    }


    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public Pair getBalance() throws RemoteException {
        CompletableFuture<Pair> future = new CompletableFuture();  
        executor.submit(()->{
            while(deliverPair == null){
            }
            future.complete(deliverPair);
        });
        try {
            return future.get();   
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BankRepository getRepository() {
        return this.repository;
    }

}