package com.example.financialplatform.ledger.repository;

import com.example.financialplatform.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByAccount_AccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<LedgerEntry> findByTransaction_Id(Long transactionId);
}
