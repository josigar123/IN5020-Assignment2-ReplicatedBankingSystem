public record BankConfig(
        String bankBindingName,
        String mdsBindingName,
        String accountName,
        int numberOfReplicas,
        String currencyFileName,
        String transactionFileName
) {
    public static BankConfig fromArgs(String[] args) {
        if (args.length < 5 || args.length > 6) {
            throw new IllegalArgumentException("[BANK] Usage: BankServer <bankBindingName> <mdsBindingName> <accountName> <numberOfReplicas> <currencyFileName> [transactionFileName]");
        }
        return new BankConfig(
                args[0],
                args[1],
                args[2],
                Integer.parseInt(args[3]),
                args[4],
                args.length == 6 ? args[5] : null
        );
    }
}

