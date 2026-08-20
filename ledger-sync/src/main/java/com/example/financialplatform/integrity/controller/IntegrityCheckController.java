package com.example.financialplatform.integrity.controller;

import com.example.financialplatform.integrity.dto.IntegrityCheckResponse;
import com.example.financialplatform.integrity.service.IntegrityCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrity-check")
@Tag(name = "Financial Integrity")
public class IntegrityCheckController {

    private final IntegrityCheckService integrityCheckService;

    public IntegrityCheckController(IntegrityCheckService integrityCheckService) {
        this.integrityCheckService = integrityCheckService;
    }

    @PostMapping
    @Operation(summary = "Verify transaction and double-entry ledger integrity")
    public IntegrityCheckResponse check() {
        return integrityCheckService.check();
    }
}
