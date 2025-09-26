import java.io.Serializable;

public class OrderedTransaction implements Serializable {

    private int order;
    private Transaction transaction;

    public OrderedTransaction(int order, Transaction transaction) {
        this.order = order;
        this.transaction = transaction;
    }
}
