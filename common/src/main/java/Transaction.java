import java.io.Serializable;

// A seralizable record class for a transaction
// A transaction has a command that must be pares, along with its unique ID
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
