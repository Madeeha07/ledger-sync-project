package com.example.financialplatform.account.dto;

import com.example.financialplatform.account.entity.AccountStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String accountNumber,
        String customerName,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt
) {
}
