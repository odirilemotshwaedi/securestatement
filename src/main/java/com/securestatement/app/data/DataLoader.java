package com.securestatement.app.data;

import com.securestatement.app.domain.entity.Transaction;
import com.securestatement.app.domain.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final CsvTransactionReader csvTransactionReader;
    private final TransactionRepository transactionRepository;

    public DataLoader(CsvTransactionReader csvTransactionReader,
                      TransactionRepository transactionRepository) {
        this.csvTransactionReader = csvTransactionReader;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (transactionRepository.count() == 0) {
            Path csvPath = Path.of("data/transaction.csv");
            List<Transaction> transactions = csvTransactionReader.read(csvPath);
            transactionRepository.saveAll(transactions);
            System.out.println("DataLoader: seeded " + transactions.size() + " transactions from CSV.");
        } else {
            System.out.println("DataLoader: transactions table already populated, skipping seed.");
        }
    }
}
