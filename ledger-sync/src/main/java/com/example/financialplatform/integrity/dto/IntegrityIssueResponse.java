package com.example.financialplatform.integrity.dto;

public record IntegrityIssueResponse(
        String transactionReference,
        String issue
) {
}
