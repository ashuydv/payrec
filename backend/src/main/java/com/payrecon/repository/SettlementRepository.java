package com.payrecon.repository;

import com.payrecon.domain.Settlement;
import com.payrecon.dto.SettlementSummaryRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByMerchantIdAndPeriod(Long merchantId, String period);

    /**
     * Native summary query joining settlements to merchants, ordered by
     * total descending. Plain read-only SELECT: no explicit locking needed
     * since it's not part of a read-modify-write sequence (unlike the
     * settlement upsert itself, which relies on @Version — see
     * SettlementServiceImpl for the lock/deadlock reasoning there).
     */
    @Query(value = """
            select m.id as merchantId, m.name as merchantName, s.period as period,
                   s.total_amount as totalAmount, s.status as status
            from settlements s
            join merchants m on m.id = s.merchant_id
            where (cast(:merchantId as bigint) is null or m.id = :merchantId)
            order by s.total_amount desc
            """, nativeQuery = true)
    List<SettlementSummaryRowProjection> findSummary(@Param("merchantId") Long merchantId);

    interface SettlementSummaryRowProjection {
        Long getMerchantId();

        String getMerchantName();

        String getPeriod();

        BigDecimal getTotalAmount();

        String getStatus();
    }

    default List<SettlementSummaryRow> summarize(Long merchantId) {
        return findSummary(merchantId).stream()
                .map(p -> new SettlementSummaryRow(p.getMerchantId(), p.getMerchantName(), p.getPeriod(), p.getTotalAmount(), p.getStatus()))
                .toList();
    }

    /**
     * Same summary as {@link #findSummary}, but sourced from the
     * settlement_summary() stored function (db/procedures.sql) instead of an
     * inline query — demonstrating calling a stored routine directly.
     */
    @Query(value = "select * from settlement_summary(cast(:merchantId as bigint))", nativeQuery = true)
    List<SettlementSummaryRowProjection> findSummaryViaStoredProcedure(@Param("merchantId") Long merchantId);

    default List<SettlementSummaryRow> summarizeViaStoredProcedure(Long merchantId) {
        return findSummaryViaStoredProcedure(merchantId).stream()
                .map(p -> new SettlementSummaryRow(p.getMerchantId(), p.getMerchantName(), p.getPeriod(), p.getTotalAmount(), p.getStatus()))
                .toList();
    }
}
