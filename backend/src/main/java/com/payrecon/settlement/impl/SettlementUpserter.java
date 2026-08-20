package com.payrecon.settlement.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.Settlement;
import com.payrecon.repository.SettlementRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Split out from SettlementServiceImpl so each of these two operations runs
 * in its own fresh REQUIRES_NEW transaction — required for two different
 * reasons:
 *
 * - tryInsert(): if it throws DataIntegrityViolationException (unique
 *   constraint on merchant_id+period), that poisons the current Hibernate
 *   session ("don't flush the Session after an exception occurs"), so the
 *   caller's subsequent fallback read needs a different session entirely —
 *   not just a caught exception.
 * - updateExisting(): if it throws ObjectOptimisticLockingFailureException
 *   (the @Version check on Settlement), the transaction that saw it is
 *   marked rollback-only and can't be reused for a retry.
 *
 * @Transactional only applies through Spring's proxy, so these methods must
 * live on a different bean than whatever orchestrates retries between them
 * (SettlementServiceImpl) — the same reason TransactionInserter is a
 * separate bean in Phase 1.
 */
@Component
class SettlementUpserter {

    private final SettlementRepository settlementRepository;

    SettlementUpserter(SettlementRepository settlementRepository) {
        this.settlementRepository = settlementRepository;
    }

    @Transactional(readOnly = true)
    Optional<Settlement> find(Long merchantId, String period) {
        return settlementRepository.findByMerchantIdAndPeriod(merchantId, period);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Settlement tryInsert(Merchant merchant, String period, BigDecimal total) {
        return settlementRepository.saveAndFlush(new Settlement(merchant, period, total));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Settlement updateExisting(Long merchantId, String period, BigDecimal total) {
        Settlement settlement = settlementRepository.findByMerchantIdAndPeriod(merchantId, period)
                .orElseThrow(() -> new IllegalStateException(
                        "Settlement for merchant %d period %s vanished mid-upsert".formatted(merchantId, period)));
        settlement.setTotalAmount(total);
        return settlementRepository.saveAndFlush(settlement);
    }
}
