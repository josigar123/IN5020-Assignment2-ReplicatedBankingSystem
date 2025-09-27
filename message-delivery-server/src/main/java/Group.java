import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Group {

    private String groupName;
    private Map<String, BankServerInfo> members = new ConcurrentHashMap<>();
    private volatile int currentBalance;

    public Group(String groupName){
        this.groupName = groupName;
    }

    public void join(BankServerInfo member){
        members.put(member.getName(), member);
    }

    public void leave(String bankName){
        members.remove(bankName);
    }

    public List<BankServerInfo> getMembers() {
        return new ArrayList<>(members.values());
    }

    public int getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(int currentBalance) {
        this.currentBalance = currentBalance;
    }
}
