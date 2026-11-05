package com.securestatement.app.domain.repository;

import com.securestatement.app.domain.entity.Transaction;
import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCustomerIdIgnoreCaseAndTimestampBetween(
        String customerId, LocalDateTime startDateTime, LocalDateTime endDateTime);
}