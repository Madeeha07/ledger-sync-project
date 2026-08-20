package com.example.financialplatform.reconciliation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reconciliation_results")
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    private ReconciliationRun run;

    @Column(length = 50, updatable = false)
    private String transactionReference;

    @Column(length = 50, updatable = false)
    private String settlementReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private ReconciliationResultType resultType;

    @Column(nullable = false, length = 500, updatable = false)
    private String description;

    protected ReconciliationResult() {
    }

    public ReconciliationResult(ReconciliationRun run, String transactionReference,
                                String settlementReference, ReconciliationResultType resultType,
                                String description) {
        this.run = run;
        this.transactionReference = transactionReference;
        this.settlementReference = settlementReference;
        this.resultType = resultType;
        this.description = description;
    }

    public Long getId() { return id; }
    public ReconciliationRun getRun() { return run; }
    public String getTransactionReference() { return transactionReference; }
    public String getSettlementReference() { return settlementReference; }
    public ReconciliationResultType getResultType() { return resultType; }
    public String getDescription() { return description; }
}
