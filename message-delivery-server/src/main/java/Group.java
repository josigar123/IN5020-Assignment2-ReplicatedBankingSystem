import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


// A class for defining a group
public class Group {

    private final String groupName;

    // A map for storting group members, their Binding name and their BankServerInfo
    private final Map<String, BankServerInfo> members = new ConcurrentHashMap<>();

    // A group might have a snapshot which can be given to new joining members
    private Pair snapshot;

    public Group(String groupName){
        this.groupName = groupName;
    }

    // Add to map
    public void join(BankServerInfo member){
        members.put(member.getName(), member);
    }

    // Remove from map
    public void leave(String bankName){
        members.remove(bankName);
    }

    // Get BankServerInfoAs list
    public List<BankServerInfo> getMembers() {
        return new ArrayList<>(members.values());
    }

    // Get bank names
    public List<String> getMembersByNames(){
        return new ArrayList<>(members.keySet());
    }

    // Get the snapshot
    public Pair getCurrentBalance() {
        return snapshot;
    }

    // Setter for snapshot
    public void setCurrentBalance(Pair snapshot) {
        this.snapshot = snapshot;
    }

    // Getter for group name
    public String getGroupName() {
        return groupName;
    }
}
