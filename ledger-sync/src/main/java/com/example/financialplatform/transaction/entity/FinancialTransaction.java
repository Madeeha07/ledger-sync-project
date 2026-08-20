package com.example.financialplatform.transaction.entity;

import com.example.financialplatform.account.entity.Account;
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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "financial_transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transaction_reference", columnNames = "reference_number"),
        @UniqueConstraint(name = "uk_transaction_idempotency", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_transaction_reversal", columnNames = "reversal_of_id")
})
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, updatable = false, length = 50)
    private String referenceNumber;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false, updatable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false, updatable = false)
    private Account destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_id", updatable = false)
    private FinancialTransaction reversalOf;

    protected FinancialTransaction() {
    }

    public FinancialTransaction(String referenceNumber, String idempotencyKey,
                                Account sourceAccount, Account destinationAccount,
                                BigDecimal amount, FinancialTransaction reversalOf) {
        this.referenceNumber = referenceNumber;
        this.idempotencyKey = idempotencyKey;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.status = TransactionStatus.PENDING;
        this.createdAt = Instant.now();
        this.reversalOf = reversalOf;
    }

    public Long getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Account getSourceAccount() { return sourceAccount; }
    public Account getDestinationAccount() { return destinationAccount; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public FinancialTransaction getReversalOf() { return reversalOf; }

    public void complete() {
        this.status = TransactionStatus.COMPLETED;
    }

    public void markReversed() {
        this.status = TransactionStatus.REVERSED;
    }
}
