#!/bin/bash

# Arguments:
# $1 = base bank name (e.g. bank-R)
# $2 = mdsBindingName
# $3 = accountName
# $4 = numberOfReplicas
# $5 = currencyFileName
# $6 = transactionFileName (optional; if empty => interactive mode)

JAR_PATH="bank-server/target/bank-server-1.0-SNAPSHOT.jar:common/target/common-1.0-SNAPSHOT.jar:message-delivery-server/target/message-delivery-server-1.0-SNAPSHOT.jar"

if [ -z "$6" ]; then
    # Interactive mode (no transaction file)
    echo "[BANK] Running single interactive BankServer..."
    java -cp "$JAR_PATH" BankServer "$1" "$2" "$3" "$4" "$5"
else
    # Batch mode (transaction file provided)
    echo "[BANK] Launching $4 BankServer replicas in batch mode..."
    for ((i=0; i< $4; i++)); do
        bankName="$1$((i+1))"  # append (i+1) to base name
        echo "[BANK] Starting $bankName..."
        java -cp "$JAR_PATH" BankServer "$bankName" "$2" "$3" "$4" "$5" "$6" &
    done

    # Optional: wait for all background processes to finish
    wait
fi