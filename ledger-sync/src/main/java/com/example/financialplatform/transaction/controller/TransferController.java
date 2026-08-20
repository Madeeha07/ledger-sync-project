package com.example.financialplatform.transaction.controller;

import com.example.financialplatform.transaction.dto.TransactionResponse;
import com.example.financialplatform.transaction.dto.TransferRequest;
import com.example.financialplatform.transaction.service.TransferService;
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
@RequestMapping("/api/transfers")
@Tag(name = "Transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer funds atomically and create balanced ledger entries")
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        return transferService.transfer(request);
    }

    @GetMapping("/{referenceNumber}")
    @Operation(summary = "Get a transaction")
    public TransactionResponse get(@PathVariable String referenceNumber) {
        return transferService.get(referenceNumber);
    }

    @PostMapping("/{referenceNumber}/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reverse a completed transaction with compensating ledger entries")
    public TransactionResponse reverse(@PathVariable String referenceNumber) {
        return transferService.reverse(referenceNumber);
    }
}
