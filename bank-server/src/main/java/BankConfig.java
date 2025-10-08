public record BankConfig(
        String bankBindingName,         // The name used to bind the bank server instance
        String mdsBindingName,          // The name used to bind the message delivery service
        String accountName,             // The name of the bank account associated with this server
        int numberOfReplicas,           // Number of replica servers 
        String currencyFileName,        // File containing currencies
        String transactionFileName      // File containing predifined transactions
) {
    // Creates a BankConfig instance by parsing cmd line arguments
    public static BankConfig fromArgs(String[] args) {
        if (args.length < 5 || args.length > 6) {
            throw new IllegalArgumentException("[BANK] Usage: BankServer <bankBindingName> <mdsBindingName> <accountName> <numberOfReplicas> <currencyFileName> [transactionFileName]");
        }

        // Parse and create a new configuration object from the provided arguments
        return new BankConfig(
                args[0],                            //BankBindingName
                args[1],                            //mdsBindingName
                args[2],                            //accountName
                Integer.parseInt(args[3]),          //numberOfReplicas
                args[4],                            //currencyFileName
                args.length == 6 ? args[5] : null   //transactionFileName
        );
    }
}

