package com.example.financialplatform.reconciliation.repository;

import com.example.financialplatform.reconciliation.entity.ReconciliationRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Long> {
}
