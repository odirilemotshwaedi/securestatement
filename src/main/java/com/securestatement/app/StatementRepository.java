package com.securestatement.app;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatementRepository extends JpaRepository<StatementMetadata, Long> {
    // You can add custom query methods here if needed
    List<StatementMetadata> findByCustomerId(String customerId);
}
