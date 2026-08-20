package com.payrecon.repository;

import com.payrecon.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByExternalReference(String externalReference);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.merchant.id = :merchantId
              and t.status = com.payrecon.domain.TransactionStatus.PROCESSED
              and t.processedAt >= :from
              and t.processedAt < :to
            """)
    BigDecimal sumProcessedAmount(
            @Param("merchantId") Long merchantId, @Param("from") Instant from, @Param("to") Instant to);
}
