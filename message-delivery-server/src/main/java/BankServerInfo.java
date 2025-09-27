public class BankServerInfo {

    private String name;
    private BankService bank;

    public BankServerInfo(BankService bank, String name) {
        this.bank = bank;
        this.name = name;
    }

    public String getName(){
        return name;
    }

    public BankService getBank() {
        return bank;
    }
}
