package com.example.financialplatform.reconciliation.entity;

public enum ReconciliationResultType {
    MATCHED,
    MISSING_IN_LEDGER,
    MISSING_IN_SETTLEMENT,
    AMOUNT_MISMATCH,
    STATUS_MISMATCH,
    DUPLICATE
}
