
// A data structure holding information about a bank, its binding name and stub on the registry
// The stub is stored since it will be used frequently, this is to offload the registry
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
