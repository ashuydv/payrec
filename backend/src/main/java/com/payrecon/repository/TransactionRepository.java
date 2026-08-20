package com.payrecon.repository;

import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByExternalReference(String externalReference);

    /**
     * Candidate set for a settlement run: transactions not yet attached to any
     * settlement, processed within the given window. "settlement is null" is
     * what makes settlement runs idempotent across reruns of the same period.
     */
    List<Transaction> findByStatusAndSettlementIsNullAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
            TransactionStatus status, Instant processedAtFrom, Instant processedAtToExclusive);
}
