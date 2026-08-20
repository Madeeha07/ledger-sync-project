package com.example.financialplatform.account.controller;

import com.example.financialplatform.account.dto.AccountResponse;
import com.example.financialplatform.account.dto.AccountStatementResponse;
import com.example.financialplatform.account.dto.CreateAccountRequest;
import com.example.financialplatform.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a financial account")
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get an account by account number")
    public AccountResponse get(@PathVariable String accountNumber) {
        return accountService.get(accountNumber);
    }

    @GetMapping("/{accountNumber}/statement")
    @Operation(summary = "Get the immutable ledger statement for an account")
    public AccountStatementResponse statement(@PathVariable String accountNumber) {
        return accountService.statement(accountNumber);
    }
}
