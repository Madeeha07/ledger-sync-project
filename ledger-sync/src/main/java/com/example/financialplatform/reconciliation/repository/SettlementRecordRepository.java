package com.example.financialplatform.reconciliation.repository;

import com.example.financialplatform.reconciliation.entity.SettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, Long> {
    List<SettlementRecord> findByRun_IdOrderByLineNumber(Long runId);
}
