package com.example.financialplatform.reconciliation.controller;

import com.example.financialplatform.reconciliation.dto.ReconciliationRunResponse;
import com.example.financialplatform.reconciliation.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reconciliation")
@Tag(name = "Reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload and reconcile an external settlement CSV file")
    public ReconciliationRunResponse upload(@RequestPart("file") MultipartFile file) {
        return reconciliationService.upload(file);
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Get a reconciliation run with all results")
    public ReconciliationRunResponse getRun(@PathVariable Long runId) {
        return reconciliationService.getRun(runId);
    }

    @GetMapping("/{runId}/mismatches")
    @Operation(summary = "Get only mismatch results for a reconciliation run")
    public ReconciliationRunResponse getMismatches(@PathVariable Long runId) {
        return reconciliationService.getMismatches(runId);
    }
}
