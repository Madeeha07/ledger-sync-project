package com.example.financialplatform.reconciliation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reconciliation_runs")
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationRunStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    protected ReconciliationRun() {
    }

    public ReconciliationRun(String fileName) {
        this.fileName = fileName;
        this.status = ReconciliationRunStatus.PROCESSING;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public ReconciliationRunStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void complete() {
        this.status = ReconciliationRunStatus.COMPLETED;
        this.completedAt = Instant.now();
    }
}
