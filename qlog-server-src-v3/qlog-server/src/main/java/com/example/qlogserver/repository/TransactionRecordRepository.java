package com.example.qlogserver.repository;

import com.example.qlogserver.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link TransactionRecord} entity.
 * Extends JpaRepository for basic CRUD operations and JpaSpecificationExecutor
 * for dynamic query capabilities used in filtering.
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long>, JpaSpecificationExecutor<TransactionRecord> {

    // JpaSpecificationExecutor provides methods like findAll(Specification<T> spec, Pageable pageable)
    // which will be used by the TransactionService for filtering.

    // Custom query methods can be added here if needed, for example:
    // List<TransactionRecord> findByTransactionKind(String transactionKind);
    // Optional<TransactionRecord> findByTransactionIdAndStatus(Long transactionId, String status);
}

