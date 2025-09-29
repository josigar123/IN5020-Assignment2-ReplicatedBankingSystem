import java.io.Serializable;

public record Transaction(String command, String uniqueId) implements Serializable {

    public Transaction{
        if(command == null || uniqueId == null){
            throw new IllegalArgumentException("Transaction command or unique id cannot be null");
        }
    }

    public String  getCommand() {
        return command;
    }
    public String getUniqueId() {
        return uniqueId;
    }
}
