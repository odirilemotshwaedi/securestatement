package com.securestatement.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import com.securestatement.app.domain.entity.StatementMetadata;
import com.securestatement.app.domain.entity.Transaction;
import com.securestatement.app.domain.repository.StatementRepository;
import com.securestatement.app.domain.repository.TransactionRepository;

class StatementServiceTest {

    @TempDir
    Path temporaryDirectory;

    private TransactionRepository transactionRepository;
    private StatementRepository statementRepository;
    private StatementService statementService;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        statementRepository = mock(StatementRepository.class);
        statementService = new StatementService();

        ReflectionTestUtils.setField(statementService, "storageDir", temporaryDirectory.toString());
        ReflectionTestUtils.setField(statementService, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(statementService, "statementRepository", statementRepository);
    }

    @Test
    void generatesPdfAndPersistsMetadataForTransactionsInDateRange() throws Exception {
        String customerId = "C101";
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);
        List<Transaction> transactions = List.of(
                new Transaction("TX-1", customerId, new BigDecimal("125.50"), "ZAR", "Cape Town",
                        LocalDateTime.of(2026, 10, 13, 9, 0)),
                new Transaction("TX-2", customerId, new BigDecimal("-25.00"), "ZAR", "Johannesburg",
                        LocalDateTime.of(2026, 10, 20, 10, 30)));
        when(transactionRepository.findByCustomerIdIgnoreCaseAndTimestampBetween(
                customerId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)))
                .thenReturn(transactions);

        StatementMetadata metadata = statementService.generateStatement(customerId, startDate, endDate);

        Path pdfPath = Path.of(metadata.getFilePath());
        assertEquals(customerId, metadata.getCustomerId());
        assertTrue(Files.isRegularFile(pdfPath));
        assertTrue(metadata.getGeneratedAt() != null);

        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            String pdfText = new PDFTextStripper().getText(document);
            assertTrue(pdfText.contains("Account Statement for Customer: " + customerId));
            assertTrue(pdfText.contains("125.50"));
            assertTrue(pdfText.contains("-25.00"));
            assertTrue(pdfText.contains("Total: 100.50"));
        }

        verify(transactionRepository).findByCustomerIdIgnoreCaseAndTimestampBetween(
                customerId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        verify(statementRepository).save(metadata);
    }

    @Test
    void rejectsStatementWhenCustomerHasNoTransactionsInDateRange() throws Exception {
        String customerId = "C101";
        LocalDate startDate = LocalDate.of(2026, 10, 30);
        LocalDate endDate = LocalDate.of(2026, 10, 31);
        when(transactionRepository.findByCustomerIdIgnoreCaseAndTimestampBetween(
                customerId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)))
                .thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> statementService.generateStatement(customerId, startDate, endDate));

        try (var files = Files.list(temporaryDirectory)) {
            assertTrue(files.findAny().isEmpty());
        }
        verify(statementRepository, never()).save(any(StatementMetadata.class));
    }
}
