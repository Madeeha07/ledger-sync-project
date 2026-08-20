package com.example.financialplatform.reconciliation.dto;

import com.example.financialplatform.reconciliation.entity.ReconciliationResultType;
import com.example.financialplatform.reconciliation.entity.ReconciliationRunStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ReconciliationRunResponse(
        Long runId,
        String fileName,
        ReconciliationRunStatus status,
        Instant createdAt,
        Instant completedAt,
        long totalResults,
        Map<ReconciliationResultType, Long> counts,
        List<ReconciliationResultResponse> results
) {
}
