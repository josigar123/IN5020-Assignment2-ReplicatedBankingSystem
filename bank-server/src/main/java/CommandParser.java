public class CommandParser {

    private final BankRepository repository;

    public CommandParser(BankRepository repository) {
        this.repository = repository;
    }

    public String[] parseTransactionFromLine(String line){
        if (line == null || line.isBlank()) {
            System.out.println("[BANK] [ERROR] Empty command");
            return null;
        }

        return line.trim().split("\\s+");
    }

    public void buildOutstandingTransactions(String command){
        String uniqueId = repository.getBankBindingName()+ ":" + repository.getOutstandingCount();
        Transaction tx = new Transaction(command, uniqueId);
        repository.addOutstandingTransaction(tx);
    }

    public void executeTransaction(String input) {
        String[] tokens = parseTransactionFromLine(input);
        String cmd = tokens[0];

        try {
            switch (cmd.toLowerCase()) {
                case "memberinfo":
                    repository.memberInfo(repository.getAccountName());
                    break;

                case "getquickbalance":
                    if (tokens.length < 2) {
                        System.out.println("[BANK] [ERROR] Usage: getQuickBalance <currency>");
                        break;
                    }
                    repository.getQuickBalance(tokens[1]);
                    break;

                case "getsyncedbalance":
                    if (tokens.length < 2) {
                        System.out.println("[BANK] [ERROR] Usage: getSyncedBalance <currency>");
                        break;
                    }
                    repository.getSyncedBalanceNaive(tokens[1]);
                    break;

                case "deposit":
                    if (tokens.length < 3) {
                        System.out.println("[BANK] [ERROR] Usage: deposit <currency> <amount>");
                        break;
                    }
                    String depositCurrency = tokens[1];
                    double depositAmount = Double.parseDouble(tokens[2]);
                    repository.deposit(depositCurrency, depositAmount);
                    System.out.println("[BANK] Deposited " + depositAmount + " " + depositCurrency);
                    break;

                case "addinterest":
                    if (tokens.length < 3) {
                        System.out.println("[BANK] [ERROR] Usage: addInterest <currency> <percent>");
                        break;
                    }
                    String interestCurrency = tokens[1];
                    double percent = Double.parseDouble(tokens[2]);
                    repository.addInterest(interestCurrency, percent);
                    System.out.println("[BANK] Added interest " + percent + "% to " + interestCurrency);
                    break;

                case "checktxstatus":
                    if (tokens.length < 2) {
                        System.out.println("[BANK] [ERROR] Usage: checkTxStatus <id>");
                        break;
                    }
                    repository.checkTxStatus(tokens[1]);
                    break;

                case "gethistory":
                    repository.getHistory();
                    break;

                case "cleanhistory":
                    repository.cleanHistory();
                    break;

                case "sleep":
                    if (tokens.length < 2) {
                        System.out.println("[BANK] [ERROR] Usage: sleep <seconds>");
                        break;
                    }

                    double seconds = Double.parseDouble(tokens[1]);
                    repository.sleep(seconds);
                    break;

                case "exit":
                    repository.exit();
                    break;

                default:
                    System.out.println("[BANK] [ERROR] Unknown command: " + cmd);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[BANK] [ERROR] Exception: " + e.getMessage());
        }
    }
}
