import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Group {

    private String groupName;
    private Map<String, BankServerInfo> members = new ConcurrentHashMap<>();
    private Map<String, Double> currencyBalances = new ConcurrentHashMap<>();

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

    public List<String> getMembersByNames(){
        return new ArrayList<>(members.keySet());
    }

    public Map<String, Double> getCurrentBalance() {
        return Map.copyOf(currencyBalances);
    }

    public void setCurrentBalance(Map<String, Double> currencyBalances) {
        this.currencyBalances = new ConcurrentHashMap<>(currencyBalances);
    }

    public String getGroupName() {
        return groupName;
    }
}
