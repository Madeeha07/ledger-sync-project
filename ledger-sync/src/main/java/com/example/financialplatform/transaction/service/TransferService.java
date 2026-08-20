package com.example.financialplatform.transaction.service;

import com.example.financialplatform.account.entity.Account;
import com.example.financialplatform.account.entity.AccountStatus;
import com.example.financialplatform.account.repository.AccountRepository;
import com.example.financialplatform.exception.BusinessRuleException;
import com.example.financialplatform.exception.ConflictException;
import com.example.financialplatform.exception.ResourceNotFoundException;
import com.example.financialplatform.ledger.entity.LedgerEntry;
import com.example.financialplatform.ledger.entity.LedgerEntryType;
import com.example.financialplatform.ledger.repository.LedgerEntryRepository;
import com.example.financialplatform.transaction.dto.TransactionResponse;
import com.example.financialplatform.transaction.dto.TransferRequest;
import com.example.financialplatform.transaction.entity.FinancialTransaction;
import com.example.financialplatform.transaction.entity.TransactionStatus;
import com.example.financialplatform.transaction.repository.FinancialTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransferService(AccountRepository accountRepository,
                           FinancialTransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        String sourceNumber = normalizeAccountNumber(request.sourceAccount());
        String destinationNumber = normalizeAccountNumber(request.destinationAccount());
        BigDecimal amount = normalizeMoney(request.amount());
        String idempotencyKey = request.idempotencyKey().trim();

        validateBasicRules(sourceNumber, destinationNumber, amount);

        FinancialTransaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return verifyAndReturnExisting(existing, sourceNumber, destinationNumber, amount);
        }

        Map<String, Account> accounts = lockAccounts(sourceNumber, destinationNumber);

        // Recheck after acquiring account locks so concurrent retries cannot create two transfers.
        existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return verifyAndReturnExisting(existing, sourceNumber, destinationNumber, amount);
        }

        Account source = accounts.get(sourceNumber);
        Account destination = accounts.get(destinationNumber);
        validateAccount(source, "Source");
        validateAccount(destination, "Destination");
        ensureSufficientBalance(source, amount);

        FinancialTransaction transaction = new FinancialTransaction(
                nextReference(), idempotencyKey, source, destination, amount, null);
        transactionRepository.save(transaction);

        applyBalancedTransfer(transaction, source, destination, amount);
        transaction.complete();

        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(String referenceNumber) {
        FinancialTransaction transaction = transactionRepository.findByReferenceNumber(referenceNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + referenceNumber));
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse reverse(String referenceNumber) {
        FinancialTransaction original = transactionRepository
                .findByReferenceNumberForUpdate(referenceNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + referenceNumber));

        if (original.getStatus() == TransactionStatus.REVERSED) {
            throw new ConflictException("Transaction has already been reversed: " + referenceNumber);
        }
        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new BusinessRuleException("Only completed transactions can be reversed");
        }

        String reversalSourceNumber = original.getDestinationAccount().getAccountNumber();
        String reversalDestinationNumber = original.getSourceAccount().getAccountNumber();
        Map<String, Account> accounts = lockAccounts(reversalSourceNumber, reversalDestinationNumber);
        Account reversalSource = accounts.get(reversalSourceNumber);
        Account reversalDestination = accounts.get(reversalDestinationNumber);

        validateAccount(reversalSource, "Reversal source");
        validateAccount(reversalDestination, "Reversal destination");
        ensureSufficientBalance(reversalSource, original.getAmount());

        FinancialTransaction reversal = new FinancialTransaction(
                nextReference(), "REVERSAL-" + original.getReferenceNumber(),
                reversalSource, reversalDestination, original.getAmount(), original);
        transactionRepository.save(reversal);

        applyBalancedTransfer(reversal, reversalSource, reversalDestination, original.getAmount());
        reversal.complete();
        original.markReversed();

        return toResponse(reversal);
    }

    private void applyBalancedTransfer(FinancialTransaction transaction, Account source,
                                       Account destination, BigDecimal amount) {
        source.debit(amount);
        destination.credit(amount);

        LedgerEntry debit = new LedgerEntry(transaction, source, LedgerEntryType.DEBIT, amount);
        LedgerEntry credit = new LedgerEntry(transaction, destination, LedgerEntryType.CREDIT, amount);

        if (debit.getAmount().compareTo(credit.getAmount()) != 0) {
            throw new IllegalStateException("Ledger debit and credit are not balanced");
        }
        ledgerEntryRepository.saveAll(List.of(debit, credit));
    }

    private Map<String, Account> lockAccounts(String firstNumber, String secondNumber) {
        List<Account> accounts = accountRepository.findAllForUpdate(List.of(firstNumber, secondNumber));
        Map<String, Account> byNumber = accounts.stream()
                .collect(Collectors.toMap(Account::getAccountNumber, Function.identity()));
        if (!byNumber.containsKey(firstNumber)) {
            throw new ResourceNotFoundException("Account not found: " + firstNumber);
        }
        if (!byNumber.containsKey(secondNumber)) {
            throw new ResourceNotFoundException("Account not found: " + secondNumber);
        }
        return byNumber;
    }

    private TransactionResponse verifyAndReturnExisting(FinancialTransaction existing,
                                                        String sourceNumber,
                                                        String destinationNumber,
                                                        BigDecimal amount) {
        boolean sameRequest = existing.getSourceAccount().getAccountNumber().equals(sourceNumber)
                && existing.getDestinationAccount().getAccountNumber().equals(destinationNumber)
                && existing.getAmount().compareTo(amount) == 0;
        if (!sameRequest) {
            throw new ConflictException("Idempotency key is already used for a different transfer");
        }
        return toResponse(existing);
    }

    private void validateBasicRules(String sourceNumber, String destinationNumber, BigDecimal amount) {
        if (sourceNumber.equals(destinationNumber)) {
            throw new BusinessRuleException("Source and destination accounts must be different");
        }
        if (amount.signum() <= 0) {
            throw new BusinessRuleException("Transfer amount must be greater than zero");
        }
    }

    private void validateAccount(Account account, String label) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(label + " account is not active: " + account.getAccountNumber());
        }
    }

    private void ensureSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient balance in account: " + account.getAccountNumber());
        }
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("Amount must have at most two decimal places");
        }
    }

    private String normalizeAccountNumber(String value) {
        return value.trim().toUpperCase();
    }

    private String nextReference() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase();
    }

    private TransactionResponse toResponse(FinancialTransaction transaction) {
        return new TransactionResponse(
                transaction.getReferenceNumber(),
                transaction.getSourceAccount().getAccountNumber(),
                transaction.getDestinationAccount().getAccountNumber(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getCreatedAt(),
                transaction.getReversalOf() == null
                        ? null : transaction.getReversalOf().getReferenceNumber());
    }
}
