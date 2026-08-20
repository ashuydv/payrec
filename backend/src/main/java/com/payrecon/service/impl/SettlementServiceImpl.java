package com.payrecon.service.impl;

import com.payrecon.domain.Settlement;
import com.payrecon.domain.SettlementStatus;
import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.PageResponse;
import com.payrecon.dto.SettlementResponse;
import com.payrecon.dto.SettlementRunResult;
import com.payrecon.exception.InvalidStatusTransitionException;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.repository.SettlementRepository;
import com.payrecon.repository.SettlementSpecifications;
import com.payrecon.repository.TransactionRepository;
import com.payrecon.service.SettlementService;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SettlementServiceImpl implements SettlementService {

    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementMerchantRunner merchantRunner;

    public SettlementServiceImpl(
            TransactionRepository transactionRepository,
            SettlementRepository settlementRepository,
            SettlementMerchantRunner merchantRunner) {
        this.transactionRepository = transactionRepository;
        this.settlementRepository = settlementRepository;
        this.merchantRunner = merchantRunner;
    }

    @Override
    public SettlementRunResult runSettlement(String period) {
        LocalDate day = LocalDate.parse(period);
        Instant windowStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant windowEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Transaction> candidates = transactionRepository
                .findByStatusAndSettlementIsNullAndProcessedAtGreaterThanEqualAndProcessedAtLessThan(
                        TransactionStatus.PROCESSED, windowStart, windowEnd);

        Map<Long, List<Long>> candidateIdsByMerchant = candidates.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getMerchant().getId(),
                        Collectors.mapping(Transaction::getId, Collectors.toList())));

        int transactionsSettled = 0;
        int merchantsSettled = 0;
        int merchantsSkippedFinalized = 0;
        int merchantsSkippedConflict = 0;

        for (Map.Entry<Long, List<Long>> entry : candidateIdsByMerchant.entrySet()) {
            try {
                SettlementMerchantRunner.Outcome outcome =
                        merchantRunner.settleMerchant(entry.getKey(), period, entry.getValue());
                switch (outcome.result()) {
                    case SETTLED -> {
                        merchantsSettled++;
                        transactionsSettled += outcome.transactionsSettled();
                    }
                    case SKIPPED_FINALIZED -> merchantsSkippedFinalized++;
                    case NO_MATCHED_TRANSACTIONS -> {
                        // Left unsettled on purpose: a future run of this same period
                        // will pick these up once they reconcile as MATCHED.
                    }
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                // A concurrent settlement run committed against this merchant/period
                // between our read and write. Don't fail the whole batch for it —
                // the next run picks this merchant back up.
                merchantsSkippedConflict++;
            }
        }

        return new SettlementRunResult(
                period, candidates.size(), transactionsSettled, merchantsSettled, merchantsSkippedFinalized, merchantsSkippedConflict);
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long id) {
        return settlementRepository.findById(id)
                .map(SettlementResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SettlementResponse> listSettlements(Long merchantId, SettlementStatus status, Pageable pageable) {
        return PageResponse.from(settlementRepository
                .findAll(SettlementSpecifications.filter(merchantId, status), pageable)
                .map(SettlementResponse::from));
    }

    @Override
    @Transactional
    public SettlementResponse updateStatus(Long id, SettlementStatus newStatus) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + id));

        validateTransition(settlement.getStatus(), newStatus);
        settlement.setStatus(newStatus);
        return SettlementResponse.from(settlement);
    }

    private void validateTransition(SettlementStatus current, SettlementStatus next) {
        if (current == next) {
            return;
        }
        boolean allowed = current == SettlementStatus.OPEN && next == SettlementStatus.FINALIZED;
        if (!allowed) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition settlement from %s to %s".formatted(current, next));
        }
    }
}
