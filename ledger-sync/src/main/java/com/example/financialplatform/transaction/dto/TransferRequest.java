package com.example.financialplatform.transaction.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank @Size(max = 40) String sourceAccount,
        @NotBlank @Size(max = 40) String destinationAccount,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 100) String idempotencyKey
) {
}
