package com.example.financialplatform.transaction;

import com.example.financialplatform.account.dto.CreateAccountRequest;
import com.example.financialplatform.account.entity.AccountStatus;
import com.example.financialplatform.account.repository.AccountRepository;
import com.example.financialplatform.account.service.AccountService;
import com.example.financialplatform.exception.BusinessRuleException;
import com.example.financialplatform.exception.ConflictException;
import com.example.financialplatform.integrity.service.IntegrityCheckService;
import com.example.financialplatform.ledger.repository.LedgerEntryRepository;
import com.example.financialplatform.transaction.dto.TransactionResponse;
import com.example.financialplatform.transaction.dto.TransferRequest;
import com.example.financialplatform.transaction.entity.TransactionStatus;
import com.example.financialplatform.transaction.repository.FinancialTransactionRepository;
import com.example.financialplatform.transaction.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransferWorkflowIntegrationTest {

    @Autowired AccountService accountService;
    @Autowired TransferService transferService;
    @Autowired AccountRepository accountRepository;
    @Autowired FinancialTransactionRepository transactionRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;
    @Autowired IntegrityCheckService integrityCheckService;

    @BeforeEach
    void createAccounts() {
        accountService.create(new CreateAccountRequest(
                "ACC001", "Asha", new BigDecimal("1000.00"), AccountStatus.ACTIVE));
        accountService.create(new CreateAccountRequest(
                "ACC002", "Ravi", new BigDecimal("500.00"), AccountStatus.ACTIVE));
    }

    @Test
    void completesBalancedTransferAndUpdatesBalances() {
        TransactionResponse response = transferService.transfer(
                new TransferRequest("ACC001", "ACC002", new BigDecimal("250.00"), "KEY-001"));

        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(accountRepository.findByAccountNumber("ACC001").orElseThrow().getBalance())
                .isEqualByComparingTo("750.00");
        assertThat(accountRepository.findByAccountNumber("ACC002").orElseThrow().getBalance())
                .isEqualByComparingTo("750.00");
        assertThat(ledgerEntryRepository.findAll()).hasSize(2);
        assertThat(integrityCheckService.check().valid()).isTrue();
    }

    @Test
    void rejectsTransferWithInsufficientBalanceWithoutChangingData() {
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest("ACC001", "ACC002", new BigDecimal("2000.00"), "KEY-002")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient balance");

        assertThat(accountRepository.findByAccountNumber("ACC001").orElseThrow().getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(transactionRepository.count()).isZero();
        assertThat(ledgerEntryRepository.count()).isZero();
    }

    @Test
    void returnsOriginalTransactionForIdempotentRetry() {
        TransferRequest request = new TransferRequest(
                "ACC001", "ACC002", new BigDecimal("100.00"), "KEY-003");

        TransactionResponse first = transferService.transfer(request);
        TransactionResponse retry = transferService.transfer(request);

        assertThat(retry.referenceNumber()).isEqualTo(first.referenceNumber());
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(ledgerEntryRepository.count()).isEqualTo(2);
    }

    @Test
    void rejectsIdempotencyKeyUsedForDifferentTransfer() {
        transferService.transfer(new TransferRequest(
                "ACC001", "ACC002", new BigDecimal("100.00"), "KEY-004"));

        assertThatThrownBy(() -> transferService.transfer(new TransferRequest(
                "ACC001", "ACC002", new BigDecimal("200.00"), "KEY-004")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("different transfer");
    }

    @Test
    void reversesCompletedTransactionWithCompensatingEntries() {
        TransactionResponse original = transferService.transfer(new TransferRequest(
                "ACC001", "ACC002", new BigDecimal("300.00"), "KEY-005"));

        TransactionResponse reversal = transferService.reverse(original.referenceNumber());

        assertThat(reversal.reversalOfReference()).isEqualTo(original.referenceNumber());
        assertThat(transferService.get(original.referenceNumber()).status())
                .isEqualTo(TransactionStatus.REVERSED);
        assertThat(accountRepository.findByAccountNumber("ACC001").orElseThrow().getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(accountRepository.findByAccountNumber("ACC002").orElseThrow().getBalance())
                .isEqualByComparingTo("500.00");
        assertThat(transactionRepository.count()).isEqualTo(2);
        assertThat(ledgerEntryRepository.count()).isEqualTo(4);
        assertThat(integrityCheckService.check().valid()).isTrue();
    }

    @Test
    void preventsSecondReversal() {
        TransactionResponse original = transferService.transfer(new TransferRequest(
                "ACC001", "ACC002", new BigDecimal("50.00"), "KEY-006"));
        transferService.reverse(original.referenceNumber());

        assertThatThrownBy(() -> transferService.reverse(original.referenceNumber()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been reversed");
    }
}
