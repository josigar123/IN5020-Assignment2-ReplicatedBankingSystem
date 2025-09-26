public class BankServerInfo {

    private BankService bank;
    private String status = "INACTIVE";
    private Long timeSinceLastAck = null;

    public BankServerInfo(BankService bank) {
        this.bank = bank;
    }

    public BankService getBank() {
        return bank;
    }
    public void setBank(BankService bank) {}

    public String getStatus() {
        return status;
    }

    public Long getTimeSinceLastAck() {
        return timeSinceLastAck;
    }
}
