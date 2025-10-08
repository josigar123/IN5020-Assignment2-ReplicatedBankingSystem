public class CommandParser {

    /*
     * CommandParser is responsible for interpreting and executing the commands 
     * given by the input file, in other words, the banking operations.
     * It parses through input strings, validates commands, and deligates actions
     * to the BankRepository.
     */


    // Reference to repo for handling bank logic and data
    private final BankRepository repository;

    // Constructor
    public CommandParser(BankRepository repository) {
        this.repository = repository;
    }

    // Splits transactions into tokens (words), returns null if input is empty or invalid
    public String[] parseTransactionFromLine(String line){
        if (line == null || line.isBlank()) {
            System.out.println("[BANK] [ERROR] Empty command");
            return null;
        }
        // Split by space, trimming extra spaces
        return line.trim().split("\\s+");
    }

    /*
     * Determines if a command should be executed immidiately or queued 
     * as an outstanding transactions.
     */
    public void buildOutstandingTransactions(String command){

        //Validates command
        boolean isValid = verifyTransaction(command);

        if(isValid) {
            String[] tokens = parseTransactionFromLine(command);
            String cmd = tokens[0];

            // Certain commands (like deposit) are queued for later execution
            switch(cmd.toLowerCase()){
                case "addinterest", "deposit", "getsyncedbalance":

                    //Create unique transaction ID and queues for later execution
                    String uniqueId = repository.getBankBindingName() + ":" + repository.getOutstandingCount();
                    Transaction tx = new Transaction(command, uniqueId);
                    repository.addOutstandingTransaction(tx);
                    break;

                // Other commands execute immidiately    
                default:
                    executeTransaction(command);
            }
        }
    }

    // Validates wheter a given command string has the correct strucure and arguments
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

    // Executes a valid banking command by delegating to BankRepository
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
