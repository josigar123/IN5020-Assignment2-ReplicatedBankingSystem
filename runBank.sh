#!/bin/bash

for ((i=0; i< $4; i++)); do
    bankName="$1$((i+1))"  # append (i+1) to the base name
    java -cp "bank-server/target/bank-server-1.0-SNAPSHOT.jar:common/target/common-1.0-SNAPSHOT.jar:message-delivery-server/target/message-delivery-server-1.0-SNAPSHOT.jar" \
        BankServer "$bankName" "$2" "$3" "$4" "$5" "$6" &
done
