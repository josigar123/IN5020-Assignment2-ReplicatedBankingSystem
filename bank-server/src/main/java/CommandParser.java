import java.util.UUID;

public class CommandParser {

    private final BankRepository repository;

    public CommandParser(BankRepository repository) {
        this.repository = repository;
    }

    public String[] parseTransactionFromLine(String line){
        if (line == null || line.isBlank()) {
            System.out.println("[ERROR] Empty command");
            return null;
        }

        return line.trim().split("\\s+");
    }

    public void buildOutstandingTransactions(String command){
        String uniqueId = repository.getBankBindingName() + repository.getOutstandingCount();
        Transaction tx = new Transaction(command, uniqueId);
        repository.addOutStandingTransaction(tx);
    }

    public String executeTransaction(String input) {

        String[] tokens = parseTransactionFromLine(input);
        String cmd = tokens[0];

        try {
            switch (cmd) {
                case "memberinfo":
                    return repository.memberInfo();

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
                    if (tokens.length < 2)
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
                    break;

                case "sleep":
                    if (tokens.length < 2) return "[ERROR] Usage: sleep <seconds>";
                    double seconds = Double.parseDouble(tokens[1]);
                    repository.sleep(seconds);
                    break;

                case "exit":
                    repository.exit();
                    break;

                default:
                    return "[ERROR] Unknown command: " + cmd;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "[ERROR] Exception: " + e.getMessage();
        }
    }
}
