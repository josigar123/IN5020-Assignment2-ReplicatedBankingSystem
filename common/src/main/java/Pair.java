
import java.io.Serializable;
import java.util.Map;

// A seralizable Pair class
// Contains a snapshot of the currencies and an order counter
// Can be used to synchronize views with new joining banks
public class Pair implements Serializable{
    private final Map<String, CurrencyInfo> currencies;
    private final Integer orderCounter;

    public Pair(Map<String, CurrencyInfo> currencies, Integer orderCounter){
        this.currencies = currencies;
        this.orderCounter = orderCounter;
    }

    public Map<String, CurrencyInfo> getCurrencies(){
        return currencies;
    }

    public Integer getOrderCounter(){
        return orderCounter;
    }

    @Override
    public boolean equals(Object o) {
        return orderCounter.equals(((Pair)o).orderCounter);
    }
}