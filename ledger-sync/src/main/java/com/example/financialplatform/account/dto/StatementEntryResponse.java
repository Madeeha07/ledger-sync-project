package com.example.financialplatform.account.dto;

import com.example.financialplatform.ledger.entity.LedgerEntryType;

import java.math.BigDecimal;
import java.time.Instant;

public record StatementEntryResponse(
        String transactionReference,
        LedgerEntryType entryType,
        BigDecimal amount,
        BigDecimal signedAmount,
        Instant createdAt
) {
}
