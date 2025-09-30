import java.util.List;
import java.util.UUID;

public class CommandParser {

    private final BankRepository repository;

    public CommandParser(BankRepository repository) {
        this.repository = repository;
    }

    public String parseAndExecute(String input) {
        if (input == null || input.isBlank()) {
            return "[ERROR] Empty command";
        }

        String[] tokens = input.trim().split("\\s+");
        String cmd = tokens[0].toLowerCase();

        try {
            switch (cmd) {
                case "memberinfo":
                    return BankServer.memberInfo();

                case "getquickbalance":
                    if (tokens.length < 2) return "[ERROR] Usage: getQuickBalance <currency>";
                    repository.getQuickBalance(tokens[1]);
                    return "OK";

                case "getsyncedbalance":
                    if (tokens.length < 2) return "[ERROR] Usage: getSyncedBalance <currency>";
                    repository.getSyncedBalanceNaive(tokens[1]);
                    return "OK";

                case "deposit":
                    if (tokens.length < 3) return "[ERROR] Usage: deposit <currency> <amount>";
                    String depositCurrency = tokens[1];
                    double depositAmount = Double.parseDouble(tokens[2]);
                    Transaction depositTx = new Transaction(UUID.randomUUID().toString(),
                                                            input); // store command string
                    repository.deposit(depositCurrency, depositAmount);
                    repository.recordExecuted(depositTx);
                    return "Deposited " + depositAmount + " " + depositCurrency;

                case "addinterest":
                    if (tokens.length < 3 && tokens.length < 2) 
                        return "[ERROR] Usage: addInterest <currency> <percent>";
                    String interestCurrency = tokens[1];
                    double percent = Double.parseDouble(tokens[2]);
                    Transaction interestTx = new Transaction(UUID.randomUUID().toString(),
                                                             input);
                    repository.addInterest(interestCurrency, percent);
                    repository.recordExecuted(interestTx);
                    return "Added interest " + percent + "% to " + interestCurrency;

                case "checktxstatus":
                    if (tokens.length < 2) return "[ERROR] Usage: checkTxStatus <id>";
                    return repository.checkTxStatus(tokens[1]);

                case "gethistory":
                    return repository.getHistory();

                case "cleanhistory":
                    repository.cleanHistory();
                    return "History cleared";

                case "sleep":
                    if (tokens.length < 2) return "[ERROR] Usage: sleep <seconds>";
                    double seconds = Double.parseDouble(tokens[1]);
                    return BankServer.sleepSeconds(seconds);

                case "exit":
                    BankServer.exitProcess();
                    return "Exiting...";

                default:
                    return "[ERROR] Unknown command: " + cmd;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "[ERROR] Exception: " + e.getMessage();
        }
    }
}
