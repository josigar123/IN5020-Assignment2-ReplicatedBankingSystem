public class CurrencyInfo{
    private final String currencyName;
    private final double rateToDollar;
    private double accountValue;

    public CurrencyInfo(String currencyName, double rateToDollar){
        this.currencyName = currencyName;
        this.rateToDollar = rateToDollar;
        accountValue = 0f;
    }

    public void add(double amount){
        accountValue += amount;
    }

    public void addInterest(double interest){
        accountValue = (accountValue * interest) + accountValue;
    }

    public double getRate(){
        return rateToDollar;
    }

    public double getAccountValue(){
        return accountValue;
    }
}