package com.example.financialplatform.transaction.dto;

import com.example.financialplatform.transaction.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String referenceNumber,
        String sourceAccount,
        String destinationAccount,
        BigDecimal amount,
        TransactionStatus status,
        Instant createdAt,
        String reversalOfReference
) {
}
