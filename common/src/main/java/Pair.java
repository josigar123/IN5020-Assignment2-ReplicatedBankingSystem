
import java.io.Serializable;
import java.util.Map;

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
}