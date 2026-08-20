package com.example.financialplatform.reconciliation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlement_records")
public class SettlementRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    private ReconciliationRun run;

    @Column(nullable = false, length = 50, updatable = false)
    private String externalReference;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 20, updatable = false)
    private String status;

    @Column(nullable = false, updatable = false)
    private int lineNumber;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    protected SettlementRecord() {
    }

    public SettlementRecord(ReconciliationRun run, String externalReference,
                            BigDecimal amount, String status, int lineNumber) {
        this.run = run;
        this.externalReference = externalReference;
        this.amount = amount;
        this.status = status;
        this.lineNumber = lineNumber;
        this.uploadedAt = Instant.now();
    }

    public Long getId() { return id; }
    public ReconciliationRun getRun() { return run; }
    public String getExternalReference() { return externalReference; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public int getLineNumber() { return lineNumber; }
    public Instant getUploadedAt() { return uploadedAt; }
}
