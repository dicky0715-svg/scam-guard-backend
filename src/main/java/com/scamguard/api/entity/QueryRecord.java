package com.scamguard.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "query_records")
public class QueryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String riskLevel;
    private String scamType;

    @Column(columnDefinition = "TEXT")
    private String advice;

    private LocalDateTime createdAt;

    // 無參數構造函數（JPA 需要）
    public QueryRecord() {}

    // 有參數構造函數
    public QueryRecord(String message, String riskLevel, String scamType, String advice) {
        this.message = message;
        this.riskLevel = riskLevel;
        this.scamType = scamType;
        this.advice = advice;
        this.createdAt = LocalDateTime.now();
    }

    // Getter 和 Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getScamType() {
        return scamType;
    }

    public void setScamType(String scamType) {
        this.scamType = scamType;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}