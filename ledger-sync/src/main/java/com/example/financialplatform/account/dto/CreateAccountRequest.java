package com.example.financialplatform.account.dto;

import com.example.financialplatform.account.entity.AccountStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,40}") String accountNumber,
        @NotBlank @Size(max = 120) String customerName,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal initialBalance,
        AccountStatus status
) {
}
