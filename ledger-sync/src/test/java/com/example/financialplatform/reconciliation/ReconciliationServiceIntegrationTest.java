package com.example.financialplatform.reconciliation;

import com.example.financialplatform.account.dto.CreateAccountRequest;
import com.example.financialplatform.account.entity.AccountStatus;
import com.example.financialplatform.account.service.AccountService;
import com.example.financialplatform.exception.InvalidCsvException;
import com.example.financialplatform.reconciliation.dto.ReconciliationRunResponse;
import com.example.financialplatform.reconciliation.entity.ReconciliationResultType;
import com.example.financialplatform.reconciliation.service.ReconciliationService;
import com.example.financialplatform.transaction.dto.TransactionResponse;
import com.example.financialplatform.transaction.dto.TransferRequest;
import com.example.financialplatform.transaction.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReconciliationServiceIntegrationTest {

    @Autowired AccountService accountService;
    @Autowired TransferService transferService;
    @Autowired ReconciliationService reconciliationService;

    private TransactionResponse transaction;

    @BeforeEach
    void prepareTransaction() {
        accountService.create(new CreateAccountRequest(
                "REC001", "Mina", new BigDecimal("900.00"), AccountStatus.ACTIVE));
        accountService.create(new CreateAccountRequest(
                "REC002", "Noor", new BigDecimal("100.00"), AccountStatus.ACTIVE));
        transaction = transferService.transfer(new TransferRequest(
                "REC001", "REC002", new BigDecimal("125.00"), "REC-KEY-001"));
    }

    @Test
    void identifiesMatchedAndMissingLedgerRecords() {
        String csv = "reference,amount,status\n"
                + transaction.referenceNumber() + ",125.00,COMPLETED\n"
                + "EXTERNAL-ONLY,40.00,COMPLETED\n";

        ReconciliationRunResponse response = reconciliationService.upload(csv(csv));

        assertThat(response.counts().get(ReconciliationResultType.MATCHED)).isEqualTo(1);
        assertThat(response.counts().get(ReconciliationResultType.MISSING_IN_LEDGER)).isEqualTo(1);
        assertThat(reconciliationService.getMismatches(response.runId()).results())
                .extracting(result -> result.resultType())
                .containsExactly(ReconciliationResultType.MISSING_IN_LEDGER);
    }

    @Test
    void identifiesAmountMismatch() {
        String csv = "reference,amount,status\n"
                + transaction.referenceNumber() + ",126.00,COMPLETED\n";

        ReconciliationRunResponse response = reconciliationService.upload(csv(csv));

        assertThat(response.counts().get(ReconciliationResultType.AMOUNT_MISMATCH)).isEqualTo(1);
    }

    @Test
    void identifiesDuplicateSettlementReferences() {
        String csv = "reference,amount,status\n"
                + transaction.referenceNumber() + ",125.00,COMPLETED\n"
                + transaction.referenceNumber() + ",125.00,COMPLETED\n";

        ReconciliationRunResponse response = reconciliationService.upload(csv(csv));

        assertThat(response.counts().get(ReconciliationResultType.DUPLICATE)).isEqualTo(1);
    }

    @Test
    void rejectsInvalidHeader() {
        assertThatThrownBy(() -> reconciliationService.upload(csv(
                "id,value,state\nA,10.00,COMPLETED\n")))
                .isInstanceOf(InvalidCsvException.class)
                .hasMessageContaining("header");
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "settlement.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
