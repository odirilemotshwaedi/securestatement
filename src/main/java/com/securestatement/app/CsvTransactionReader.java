package com.securestatement.app;

import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;


@Component
public class CsvTransactionReader {
    
    private static final String EXPECTED_HEADER = "transaction_id,customer_id,amount,currency,location,timestamp";
    private static final int EXPECTED_COLUMN_COUNT = 6;

    private static final int TRANSACTION_ID_COLUMN = 0;
    private static final int CUSTOMER_ID_COLUMN = 1;
    private static final int AMOUNT_COLUMN = 2;
    private static final int CURRENCY_COLUMN = 3;
    private static final int LOCATION_COLUMN = 4;
    private static final int TIMESTAMP_COLUMN = 5;

    private final List<String> errors = new ArrayList<>();

    public List<Transaction> read(Path csvFile) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        errors.clear();

        try(BufferedReader reader = Files.newBufferedReader(csvFile)){
            String headerLine = reader.readLine();
            int lineNumber = 1;

            if (headerLine == null){
                errors.add("The CSV file is empty.");
                return transactions;
            }
        
            if(!EXPECTED_HEADER.equals(headerLine.trim())){
                errors.add("Line " + lineNumber + ": unexpected header \"" + headerLine + "\".");
                return transactions;
            }

            String line;
            while ((line = reader.readLine()) != null){
                lineNumber++;

                if(line.isBlank()){
                    continue; // Skip empty lines
                }

                Transaction transaction = parseRow(line, lineNumber);
                if (transaction != null){
                    transactions.add(transaction);
                }

            }

        }
        return transactions;
    }

    public List<String> getErrors(){
        return errors;
    }

    /**
     * converts one data line into a Transaction.
     * Return  null when the line cannot be parsed, after recording an error.
     */
    private Transaction parseRow(String line, int lineNumber){
        //The limit keeps an empty field at the end of the line in the array.
        String[] fields = line.split(",", -1);
        if(fields.length != EXPECTED_COLUMN_COUNT){
            errors.add("Line "+ lineNumber + ": expected " + EXPECTED_COLUMN_COUNT + " fields but found " + fields.length + ".");
            return null;
        }

        String transactionId = fields[TRANSACTION_ID_COLUMN].trim();
        String customerId = fields[CUSTOMER_ID_COLUMN].trim();
        String amountText = fields[AMOUNT_COLUMN].trim();
        String currency = fields[CURRENCY_COLUMN].trim();
        String location = fields[LOCATION_COLUMN].trim();
        String timestampText = fields[TIMESTAMP_COLUMN].trim();

        //a negative amount such as -50.00 is a valid decimal. Only non-numberic text fails here
        BigDecimal amount;
        try{
            amount = new BigDecimal(amountText);
        } catch (NumberFormatException e){
            errors.add("Line " + lineNumber + ": could not parse amount \"" + amountText + "\" as a decimal number.");
            return null;
        }

        LocalDateTime timestamp;
        try{
            timestamp = LocalDateTime.parse(timestampText);
        } catch (DateTimeParseException e){
            errors.add("Line " + lineNumber + ": could not parse timestamp \"" + timestampText + "\" as a date and time.");
            return null;
        }
    
        return new Transaction(transactionId, customerId, amount, currency, location, timestamp);
    }
}
