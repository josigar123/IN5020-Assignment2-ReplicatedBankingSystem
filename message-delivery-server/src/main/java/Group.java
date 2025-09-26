import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class Group {

    private String groupName;
    private List<BankServerInfo> members;

    public Group(String groupName){
        this.groupName = groupName;
        this.members = Collections.synchronizedList(new ArrayList<>());
    }

    public void join(BankServerInfo member){
        members.add(member);
    }

    public void leave(BankServerInfo member){
        members.remove(member);
    }

    public List<BankServerInfo> getMembers() {
        // return a copy to avoid concurrent modification
        synchronized (members) {
            return new ArrayList<>(members);
        }
    }
}
