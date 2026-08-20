package com.example.financialplatform.integrity.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record IntegrityCheckResponse(
        boolean valid,
        Instant checkedAt,
        long transactionsChecked,
        long ledgerEntriesChecked,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        List<IntegrityIssueResponse> issues
) {
}
