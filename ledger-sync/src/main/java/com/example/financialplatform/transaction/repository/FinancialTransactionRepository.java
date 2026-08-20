package com.example.financialplatform.transaction.repository;

import com.example.financialplatform.transaction.entity.FinancialTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    Optional<FinancialTransaction> findByReferenceNumber(String referenceNumber);
    Optional<FinancialTransaction> findByIdempotencyKey(String idempotencyKey);
    List<FinancialTransaction> findAllByOrderByCreatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from FinancialTransaction t where t.referenceNumber = :referenceNumber")
    Optional<FinancialTransaction> findByReferenceNumberForUpdate(@Param("referenceNumber") String referenceNumber);
}
