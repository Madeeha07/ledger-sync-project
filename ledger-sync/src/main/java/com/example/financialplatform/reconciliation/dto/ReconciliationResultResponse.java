package com.example.financialplatform.reconciliation.dto;

import com.example.financialplatform.reconciliation.entity.ReconciliationResultType;

public record ReconciliationResultResponse(
        String transactionReference,
        String settlementReference,
        ReconciliationResultType resultType,
        String description
) {
}
