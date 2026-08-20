package com.payrecon.settlement.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.Settlement;
import com.payrecon.dto.SettlementResponse;
import com.payrecon.exception.ConcurrentSettlementException;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.MerchantRepository;
import com.payrecon.repository.SettlementRepository;
import com.payrecon.repository.TransactionRepository;
import com.payrecon.settlement.SettlementService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/*
 * Concurrency strategy
 * =====================
 * Two settlement runs for the same merchant/period can legitimately race —
 * e.g. an ops user manually re-triggers a settlement while the nightly
 * scheduled run is still in flight. There are two distinct races here,
 * handled two different ways:
 *
 * 1. CREATE race: no Settlement row exists yet for (merchantId, period), and
 *    two concurrent calls both try to insert one. This is the same shape of
 *    problem as Phase 1's idempotent transaction creation, and is solved the
 *    same way: the DB unique constraint on (merchant_id, period) is the
 *    source of truth. We optimistically insert; on a constraint violation we
 *    know we lost the race, so we fall through to the update path instead.
 *
 * 2. UPDATE race: a Settlement row already exists and two concurrent calls
 *    both try to recompute/overwrite its totalAmount. This is where
 *    optimistic locking (@Version on Settlement) matters: both transactions
 *    read the same version, both compute a new total, but only the first
 *    UPDATE ... WHERE version = ? commits — the second finds zero rows
 *    matched, and Hibernate raises ObjectOptimisticLockingFailureException.
 *    We do NOT catch this and blindly retry-forever, because that would let
 *    the loser silently recompute against a total the winner already
 *    changed, which could still be right (settlement is idempotent per
 *    period, computed from source transactions, not incremented) — but to
 *    keep the behavior demonstrable and bounded, we retry a small fixed
 *    number of times and then surface ConcurrentSettlementException (409) so
 *    the caller knows a concurrent run happened rather than the retry
 *    silently masking it forever.
 *
 * The find/insert/update primitives live on SettlementUpserter, each in its
 * own REQUIRES_NEW transaction — see that class for why a single shared
 * transaction can't be reused across the insert-fails-then-recover or
 * lock-fails-then-retry paths.
 */
@Service
public class SettlementServiceImpl implements SettlementService {

    private static final int MAX_OPTIMISTIC_LOCK_RETRIES = 3;

    private final SettlementRepository settlementRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final SettlementUpserter settlementUpserter;

    public SettlementServiceImpl(
            SettlementRepository settlementRepository,
            MerchantRepository merchantRepository,
            TransactionRepository transactionRepository,
            SettlementUpserter settlementUpserter) {
        this.settlementRepository = settlementRepository;
        this.merchantRepository = merchantRepository;
        this.transactionRepository = transactionRepository;
        this.settlementUpserter = settlementUpserter;
    }

    @Override
    public SettlementResponse runSettlement(Long merchantId, String period) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + merchantId));

        Instant[] range = periodRange(period);
        BigDecimal total = transactionRepository.sumProcessedAmount(merchantId, range[0], range[1]);

        for (int attempt = 1; attempt <= MAX_OPTIMISTIC_LOCK_RETRIES; attempt++) {
            try {
                // Pass the already-loaded merchant explicitly: the Settlement
                // returned by upsert() came from a REQUIRES_NEW transaction
                // that has already committed and closed its session, so its
                // lazy Settlement.merchant association can't be dereferenced
                // here — only fields already loaded on `merchant` itself can.
                return SettlementResponse.from(upsert(merchant, period, total), merchant);
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == MAX_OPTIMISTIC_LOCK_RETRIES) {
                    throw new ConcurrentSettlementException(
                            "Settlement for merchant %d period %s was modified concurrently; retries exhausted"
                                    .formatted(merchantId, period), e);
                }
                // Another settlement run committed first; loop and retry against the fresh row.
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    private Settlement upsert(Merchant merchant, String period, BigDecimal total) {
        boolean exists = settlementUpserter.find(merchant.getId(), period).isPresent();

        if (!exists) {
            try {
                return settlementUpserter.tryInsert(merchant, period, total);
            } catch (DataIntegrityViolationException e) {
                // Lost the create race to a concurrent upsert; fall through
                // to the update path below against the row that won.
                // updateExisting() runs in its own fresh transaction/session,
                // since the one that just failed here is no longer usable.
            }
        }

        return settlementUpserter.updateExisting(merchant.getId(), period, total);
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long merchantId, String period) {
        return settlementRepository.findByMerchantIdAndPeriod(merchantId, period)
                .map(SettlementResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No settlement found for merchant %d period %s".formatted(merchantId, period)));
    }

    private Instant[] periodRange(String period) {
        LocalDate day = LocalDate.parse(period);
        Instant from = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return new Instant[]{from, to};
    }
}
