#!/bin/bash

echo "RUNNING FIRST BATCH"
./runBank.sh bank-R message-delivery-service group3 3 FinalInputFiles/TradingRate.txt FinalInputFiles/Rep1.txt &


sleep 5

echo "RUNNING SECOND BATCH"
./runBank.sh bank-R2 message-delivery-service group3 1 FinalInputFiles/TradingRate.txt FinalInputFiles/Rep2.txt &


sleep 15

echo "RUNNING THIRD BATCH"
./runBank.sh bank-R3 message-delivery-service group3 1 FinalInputFiles/TradingRate.txt FinalInputFiles/Rep3.txt &
