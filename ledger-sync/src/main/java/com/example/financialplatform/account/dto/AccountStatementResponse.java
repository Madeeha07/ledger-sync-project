package com.example.financialplatform.account.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccountStatementResponse(
        String accountNumber,
        String customerName,
        BigDecimal currentBalance,
        List<StatementEntryResponse> entries
) {
}
