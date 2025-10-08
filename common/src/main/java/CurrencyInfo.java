
import java.io.Serializable;

// A class that is seralizable containing information about currencies,
// making acessing, manipulation and conversion simpler. Class is self explanatory
public class CurrencyInfo implements Serializable{
    private final String currencyName;
    private final double rateToDollar;
    private double accountValue;

    // Constructor
    public CurrencyInfo(String currencyName, double rateToDollar){
        this.currencyName = currencyName;
        this.rateToDollar = rateToDollar;
        accountValue = 0f;
    }

    // Add amount to currency
    public void add(double amount){
        accountValue += amount;
    }

    // Add interest to currency
    public void addInterest(double interest){
        accountValue = (accountValue * interest) + accountValue;
    }

    // Get currencies rate
    public double getRate(){
        return rateToDollar;
    }

    // Get the value for currency
    public double getAccountValue(){
        return accountValue;
    }
}