public class BankServerInfo {

    private final String name;
    private final BankService bank;

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
