package com.example.financialplatform.account.service;

import com.example.financialplatform.account.dto.AccountResponse;
import com.example.financialplatform.account.dto.AccountStatementResponse;
import com.example.financialplatform.account.dto.CreateAccountRequest;
import com.example.financialplatform.account.dto.StatementEntryResponse;
import com.example.financialplatform.account.entity.Account;
import com.example.financialplatform.account.entity.AccountStatus;
import com.example.financialplatform.account.repository.AccountRepository;
import com.example.financialplatform.exception.ConflictException;
import com.example.financialplatform.exception.ResourceNotFoundException;
import com.example.financialplatform.ledger.entity.LedgerEntryType;
import com.example.financialplatform.ledger.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        String accountNumber = request.accountNumber().trim().toUpperCase();
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new ConflictException("Account number already exists: " + accountNumber);
        }
        BigDecimal balance = request.initialBalance().setScale(2, RoundingMode.UNNECESSARY);
        Account account = new Account(accountNumber, request.customerName().trim(), balance,
                request.status() == null ? AccountStatus.ACTIVE : request.status());
        return toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String accountNumber) {
        return toResponse(findAccount(accountNumber));
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse statement(String accountNumber) {
        Account account = findAccount(accountNumber);
        List<StatementEntryResponse> entries = ledgerEntryRepository
                .findByAccount_AccountNumberOrderByCreatedAtDesc(account.getAccountNumber())
                .stream()
                .map(entry -> new StatementEntryResponse(
                        entry.getTransaction().getReferenceNumber(),
                        entry.getEntryType(),
                        entry.getAmount(),
                        entry.getEntryType() == LedgerEntryType.DEBIT
                                ? entry.getAmount().negate() : entry.getAmount(),
                        entry.getCreatedAt()))
                .toList();
        return new AccountStatementResponse(account.getAccountNumber(), account.getCustomerName(),
                account.getBalance(), entries);
    }

    private Account findAccount(String accountNumber) {
        String normalized = accountNumber.trim().toUpperCase();
        return accountRepository.findByAccountNumber(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + normalized));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getAccountNumber(), account.getCustomerName(), account.getBalance(),
                account.getStatus(), account.getCreatedAt());
    }
}
