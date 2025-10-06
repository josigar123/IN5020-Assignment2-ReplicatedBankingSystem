import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Group {

    private final String groupName;
    private final Map<String, BankServerInfo> members = new ConcurrentHashMap<>();
    private Pair snapshot;

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

    public Map<String, BankServerInfo> getMembersAsMap(){
        return members;
    }

    public Pair getCurrentBalance() {
        return snapshot;
    }

    public void setCurrentBalance(Pair snapshot) {
        this.snapshot = snapshot;
    }

    public String getGroupName() {
        return groupName;
    }
}
