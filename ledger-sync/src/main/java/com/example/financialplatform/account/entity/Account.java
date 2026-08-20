package com.example.financialplatform.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String accountNumber;

    @Column(nullable = false, length = 120)
    private String customerName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Account() {
    }

    public Account(String accountNumber, String customerName, BigDecimal balance, AccountStatus status) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.balance = balance;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getCustomerName() { return customerName; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
