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
        boolean isValid = verifyTransaction(command);

        if(isValid) {
            String[] tokens = parseTransactionFromLine(command);
            String cmd = tokens[0];
            switch(cmd.toLowerCase()){
                case "addinterest", "deposit", "getsyncedbalance":
                    String uniqueId = repository.getBankBindingName() + ":" + repository.getOutstandingCount();
                    Transaction tx = new Transaction(command, uniqueId);
                    repository.addOutstandingTransaction(tx);
                    break;
                default:
                    executeTransaction(command);
            }
        }
    }

    public boolean verifyTransaction(String input){
        String[] tokens = parseTransactionFromLine(input);
        String cmd = tokens[0];

        try {
            switch (cmd.toLowerCase()) {
                case "memberinfo", "gethistory", "cleanhistory", "exit", "exitinteractive", "getsyncedbalance", "getquickbalance":
                    return true;
                case "deposit":
                    if (tokens.length < 3) {
                        System.out.println("[BANK] [ERROR] Usage: deposit <currency> <amount>");
                        break;
                    }
                    return true;
                case "addinterest":
                    if (tokens.length < 2) {
                        System.out.println("[BANK] [ERROR] Usage: addInterest [currency] <percent>");
                        break;
                    }
                    return true;
                case "checktxstatus":
                    if (tokens.length < 2) {
                        System.out.println("[BANK] [ERROR] Usage: checkTxStatus <id>");
                        break;
                    }
                    return true;
                case "sleep":
                    if (tokens.length < 2) {
                        System.out.println("[BANK] [ERROR] Usage: sleep <seconds>");
                        break;
                    }
                    return true;
                default:
                    System.out.println("[BANK] [ERROR] Unknown command: " + cmd);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[BANK] [ERROR] Exception: " + e.getMessage());
        }

        return false;
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
                    if(tokens.length == 1){
                        repository.getQuickBalance(null);
                        break;
                    }

                    repository.getQuickBalance(tokens[1]);
                    break;

                case "getsyncedbalance":
                    if(tokens.length == 1){
                        repository.getSyncedBalanceSmart(null);
                        break;
                    }

                    repository.getSyncedBalanceSmart(tokens[1]);
                    //repository.getSyncedBalanceNaive(tokens[1]);
                    break;

                case "deposit":
                    String depositCurrency = tokens[1];
                    double depositAmount = Double.parseDouble(tokens[2]);
                    repository.deposit(depositCurrency, depositAmount);
                    System.out.println("[BANK] Deposited " + depositAmount + " " + depositCurrency);
                    break;

                case "addinterest":
                    if(tokens.length == 2){
                        double percent = Double.parseDouble(tokens[1]);
                        repository.addInterest(null, percent);
                        System.out.println("[BANK] Added interest " + percent + "% to all currencies");
                    }
                    else{
                        String interestCurrency = tokens[1];
                        double percent = Double.parseDouble(tokens[2]);
                        repository.addInterest(interestCurrency, percent);
                        System.out.println("[BANK] Added interest " + percent + "% to " + interestCurrency);
                    }
                    
                    break;

                case "checktxstatus":
                    repository.checkTxStatus(tokens[1]);
                    break;

                case "gethistory":
                    repository.getHistory();
                    break;

                case "cleanhistory":
                    repository.cleanHistory();
                    break;

                case "sleep":
                    double seconds = Double.parseDouble(tokens[1]);
                    repository.sleep(seconds);
                    break;

                case "exit":
                    repository.exit(false);
                    break;
                case "exitinteractive":
                    repository.exit(true);
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
