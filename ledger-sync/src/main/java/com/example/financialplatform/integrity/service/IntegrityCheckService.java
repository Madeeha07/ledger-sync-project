package com.example.financialplatform.integrity.service;

import com.example.financialplatform.integrity.dto.IntegrityCheckResponse;
import com.example.financialplatform.integrity.dto.IntegrityIssueResponse;
import com.example.financialplatform.ledger.entity.LedgerEntry;
import com.example.financialplatform.ledger.entity.LedgerEntryType;
import com.example.financialplatform.ledger.repository.LedgerEntryRepository;
import com.example.financialplatform.transaction.entity.FinancialTransaction;
import com.example.financialplatform.transaction.entity.TransactionStatus;
import com.example.financialplatform.transaction.repository.FinancialTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntegrityCheckService {

    private final FinancialTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public IntegrityCheckService(FinancialTransactionRepository transactionRepository,
                                 LedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public IntegrityCheckResponse check() {
        List<FinancialTransaction> transactions = transactionRepository.findAllByOrderByCreatedAtAsc();
        List<IntegrityIssueResponse> issues = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO.setScale(2);
        BigDecimal totalCredits = BigDecimal.ZERO.setScale(2);
        long ledgerEntriesChecked = 0;

        for (FinancialTransaction transaction : transactions) {
            List<LedgerEntry> entries = ledgerEntryRepository.findByTransaction_Id(transaction.getId());
            ledgerEntriesChecked += entries.size();
            totalDebits = totalDebits.add(sum(entries, LedgerEntryType.DEBIT));
            totalCredits = totalCredits.add(sum(entries, LedgerEntryType.CREDIT));

            if (transaction.getStatus() == TransactionStatus.PENDING) {
                issues.add(issue(transaction, "Transaction is still PENDING"));
                continue;
            }
            if (transaction.getStatus() == TransactionStatus.FAILED) {
                if (!entries.isEmpty()) {
                    issues.add(issue(transaction, "FAILED transaction must not have ledger entries"));
                }
                continue;
            }

            if (entries.size() != 2) {
                issues.add(issue(transaction, "Expected exactly two ledger entries but found " + entries.size()));
                continue;
            }

            List<LedgerEntry> debits = entries.stream()
                    .filter(entry -> entry.getEntryType() == LedgerEntryType.DEBIT).toList();
            List<LedgerEntry> credits = entries.stream()
                    .filter(entry -> entry.getEntryType() == LedgerEntryType.CREDIT).toList();
            if (debits.size() != 1 || credits.size() != 1) {
                issues.add(issue(transaction, "Expected one DEBIT and one CREDIT entry"));
                continue;
            }

            LedgerEntry debit = debits.get(0);
            LedgerEntry credit = credits.get(0);
            if (debit.getAmount().compareTo(credit.getAmount()) != 0
                    || debit.getAmount().compareTo(transaction.getAmount()) != 0) {
                issues.add(issue(transaction, "Debit, credit, and transaction amounts are not equal"));
            }
            if (!debit.getAccount().getId().equals(transaction.getSourceAccount().getId())) {
                issues.add(issue(transaction, "Debit entry is not assigned to the source account"));
            }
            if (!credit.getAccount().getId().equals(transaction.getDestinationAccount().getId())) {
                issues.add(issue(transaction, "Credit entry is not assigned to the destination account"));
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            issues.add(new IntegrityIssueResponse(null,
                    "Global ledger is unbalanced: debits=" + totalDebits + ", credits=" + totalCredits));
        }

        return new IntegrityCheckResponse(issues.isEmpty(), Instant.now(), transactions.size(),
                ledgerEntriesChecked, totalDebits, totalCredits, List.copyOf(issues));
    }

    private BigDecimal sum(List<LedgerEntry> entries, LedgerEntryType type) {
        return entries.stream()
                .filter(entry -> entry.getEntryType() == type)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    private IntegrityIssueResponse issue(FinancialTransaction transaction, String description) {
        return new IntegrityIssueResponse(transaction.getReferenceNumber(), description);
    }
}
