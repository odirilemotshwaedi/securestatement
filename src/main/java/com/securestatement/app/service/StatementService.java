package com.securestatement.app.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.securestatement.app.domain.entity.StatementMetadata;
import com.securestatement.app.domain.entity.Transaction;
import com.securestatement.app.domain.repository.StatementRepository;
import com.securestatement.app.domain.repository.TransactionRepository;
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

    //Method generates and stores customer account statements as PDF files
    public StatementMetadata generateStatement(String customerId, LocalDate startDate, LocalDate endDate) throws IOException {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID must not be blank.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<Transaction> transactions = transactionRepository.findByCustomerIdIgnoreCaseAndTimestampBetween(
                customerId, startDateTime, endDateTime);
        System.out.println(transactions);
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

                    // Draw a line under the header
                    content.moveTo(50, 745);
                    content.lineTo(550, 745);
                    content.stroke();

                    //Column headers
                    content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    content.beginText();
                    content.newLineAtOffset(50, 720);
                    content.showText("Date");
                    content.newLineAtOffset(150, 0);
                    content.showText("Location");
                    content.newLineAtOffset(150, 0);
                    content.showText("Currency");
                    content.newLineAtOffset(80, 0);

                    content.showText("Amount");
                    content.endText();


                    content.setFont(PDType1Font.HELVETICA, 10);
                    int y = 700;
                    content.setFont(PDType1Font.HELVETICA, 10);
                    for (int i = startIndex; i < endIndex; i++) {
                        Transaction tx = transactions.get(i);

                        content.beginText();
                        content.newLineAtOffset(50, y);
                        content.showText(tx.getTimestamp().toString());
                        content.endText();

                        content.beginText();
                        content.newLineAtOffset(200, y);
                        content.showText(tx.getLocation());
                        content.endText();

                        content.beginText();
                        content.newLineAtOffset(350, y);
                        content.showText(tx.getCurrency());
                        content.endText();

                        content.beginText();
                        content.newLineAtOffset(430, y);
                        content.showText(tx.getAmount().toString());
                        content.endText();

                        y -= 18;
                    }

                    if (pageIndex == pageCount - 1) {
                        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        content.beginText();
                        content.newLineAtOffset(50, y - 20);
                        content.showText("Total: R" + total);
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
