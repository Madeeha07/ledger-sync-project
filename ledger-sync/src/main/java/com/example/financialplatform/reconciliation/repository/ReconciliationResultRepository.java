package com.example.financialplatform.reconciliation.repository;

import com.example.financialplatform.reconciliation.entity.ReconciliationResult;
import com.example.financialplatform.reconciliation.entity.ReconciliationResultType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {
    List<ReconciliationResult> findByRun_IdOrderById(Long runId);
    List<ReconciliationResult> findByRun_IdAndResultTypeNotOrderById(
            Long runId, ReconciliationResultType resultType);
}
