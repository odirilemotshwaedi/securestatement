package com.securestatement.app;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class StatementService {

    @Value("${statement.storage-dir}")
    private String storageDir;

    @Autowired 
    private TransactionRepository transactionRepository;
    @Autowired 
    private StatementRepository statementRepository;

    public StatementMetadata generateStatement(String customerId, LocalDate startDate, LocalDate endDate) throws IOException {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID must not be blank.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<Transaction> transactions = transactionRepository.findByCustomerIdIgnoreCaseAndTimestampBetween(
                customerId, startDateTime, endDateTime);

        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("No transactions found for customer: " + customerId);
        }

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        new File(storageDir).mkdirs(); // Ensure the storage directory exists
        String filePath = storageDir + File.separator + customerId + "_" + System.currentTimeMillis() + ".pdf";

        try (PDDocument document = new PDDocument()) {
            int rowsPerPage = 32;
            int pageCount = Math.max(1, (transactions.size() + rowsPerPage - 1) / rowsPerPage);

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                PDPage page = new PDPage();
                document.addPage(page);

                int startIndex = pageIndex * rowsPerPage;
                int endIndex = Math.min(startIndex + rowsPerPage, transactions.size());

                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                    content.beginText();
                    content.newLineAtOffset(50, 750);
                    content.showText("Account Statement for Customer: " + customerId);
                    content.endText();

                    content.setFont(PDType1Font.HELVETICA, 10);
                    int y = 710;
                    for (int i = startIndex; i < endIndex; i++) {
                        Transaction tx = transactions.get(i);
                        content.beginText();
                        content.newLineAtOffset(50, y);
                        content.showText(tx.getTimestamp() + " | " + tx.getLocation() + " | "
                                + tx.getCurrency() + " " + tx.getAmount());
                        content.endText();
                        y -= 18;
                    }

                    if (pageIndex == pageCount - 1) {
                        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        content.beginText();
                        content.newLineAtOffset(50, y - 20);
                        content.showText("Total: " + total);
                        content.endText();
                    }
                }
            }
            document.save(filePath);
        }

        // Build and persist metadata
        StatementMetadata metadata = new StatementMetadata();
        metadata.setCustomerId(customerId);
        metadata.setFilePath(filePath);
        metadata.setGeneratedAt(java.time.LocalDateTime.now());

        statementRepository.save(metadata);

        return metadata; 
    }

    
}






/*
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;



public class StatementService {

    private final CsvTransactionReader csvTransactionReader;
    private final Path transactionsFile;
    private final Path storageDirectory;

    @Autowired
    public StatementService(
            CsvTransactionReader csvTransactionReader,
            @Value("${statement.transactions-file:data/transactions.csv}") String transactionsFile,
            @Value("${statement.storage-directory:statements}") String storageDirectory) {
        this.csvTransactionReader = csvTransactionReader;
        this.transactionsFile = Path.of(transactionsFile);
        this.storageDirectory = Path.of(storageDirectory);
    }

    public StatementService(CsvTransactionReader csvTransactionReader) {
        this(csvTransactionReader, "data/transactions.csv", "statements");
    }

    public StatementService(CsvTransactionReader csvTransactionReader, Path transactionsFile, Path storageDirectory) {
        this.csvTransactionReader = csvTransactionReader;
        this.transactionsFile = transactionsFile;
        this.storageDirectory = storageDirectory;
    }

    public File generateStatement(String customerId, StatementRequest request) throws IOException {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID must not be blank.");
        }
        if (request == null || request.startDate() == null || request.endDate() == null
                || request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("Statement dates must be present and startDate must not be after endDate.");
        }

        List<Transaction> allTransactions = csvTransactionReader.read(transactionsFile);
        List<Transaction> transactions = allTransactions.stream()
                .filter(tx -> tx.getCustomerId().equalsIgnoreCase(customerId.trim()))
                .filter(tx -> !tx.getTimestamp().toLocalDate().isBefore(request.startDate())
                        && !tx.getTimestamp().toLocalDate().isAfter(request.endDate()))
                .toList();

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Files.createDirectories(storageDirectory);
        String statementId = UUID.randomUUID().toString();
        File file = storageDirectory.resolve(statementId + ".pdf").toFile();

        try(PDDocument document = new PDDocument()){
            int rowsPerPage = 32;
            int pageCount = Math.max(1, (transactions.size() + rowsPerPage - 1) / rowsPerPage);
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                int startIndex = pageIndex * rowsPerPage;
                int endIndex = Math.min(startIndex + rowsPerPage, transactions.size());
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                    content.beginText();
                    content.newLineAtOffset(50, 750);
                    content.showText("Account Statement for customer: " + customerId);
                    content.endText();

                    content.setFont(PDType1Font.HELVETICA, 10);
                    int y = 710;
                    for (int i = startIndex; i < endIndex; i++) {
                        Transaction tx = transactions.get(i);
                        content.beginText();
                        content.newLineAtOffset(50, y);
                        content.showText(tx.getTimestamp() + " | " + tx.getLocation() + " | "
                                + tx.getCurrency() + " " + tx.getAmount());
                        content.endText();
                        y -= 18;
                    }

                    if (pageIndex == pageCount - 1) {
                        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        content.beginText();
                        content.newLineAtOffset(50, y - 20);
                        content.showText("Total: " + total);
                        content.endText();
                    }
                }
            }
            document.save(file);
        }
        return file;
    }

}

*/

/* 
package com.securestatement.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class StatementService {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — safe for stateless REST APIs using Basic Auth
            .csrf(csrf -> csrf.disable())
            // Require authentication on every request
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            // Keep HTTP Basic Auth active
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
*/