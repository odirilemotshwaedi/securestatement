package com.securestatement.app;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class StatementMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;
    private String filePath;
    private LocalDateTime generatedAt;
    
    // --- Constructors ---
    public StatementMetadata() {}

    public StatementMetadata(String customerId, String filePath, LocalDateTime generatedAt) {
        this.customerId = customerId;
        this.filePath = filePath;
        this.generatedAt = generatedAt;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
