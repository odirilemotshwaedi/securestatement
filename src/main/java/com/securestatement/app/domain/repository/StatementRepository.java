package com.securestatement.app.domain.repository;

import com.securestatement.app.domain.entity.StatementMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatementRepository extends JpaRepository<StatementMetadata, Long> {
    List<StatementMetadata> findByCustomerId(String customerId);
}
