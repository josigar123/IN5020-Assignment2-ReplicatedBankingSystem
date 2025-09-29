#!/bin/bash

java -cp "bank-server/target/bank-server-1.0-SNAPSHOT.jar:common/target/common-1.0-SNAPSHOT.jar:message-delivery-server/target/message-delivery-server-1.0-SNAPSHOT.jar" BankServer $1 $2 $3 $4 $5 $6

