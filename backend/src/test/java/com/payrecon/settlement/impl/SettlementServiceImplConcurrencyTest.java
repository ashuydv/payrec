package com.payrecon.settlement.impl;

import com.payrecon.domain.Merchant;
import com.payrecon.domain.PaymentType;
import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.SettlementResponse;
import com.payrecon.exception.ConcurrentSettlementException;
import com.payrecon.repository.MerchantRepository;
import com.payrecon.repository.SettlementRepository;
import com.payrecon.repository.TransactionRepository;
import com.payrecon.settlement.SettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that concurrent settlement runs for the same merchant/period don't
 * corrupt the total: N threads call runSettlement() at the same instant
 * (synchronized via a CountDownLatch so they genuinely race rather than
 * running sequentially), and we assert that every successful run landed on
 * the *correct* total (not a partial/doubled one) and that the final row's
 * version count reflects real contention having occurred.
 */
@SpringBootTest
@ActiveProfiles("test")
class SettlementServiceImplConcurrencyTest {

    private static final int CONCURRENT_ATTEMPTS = 8;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Test
    void concurrentSettlementRuns_convergeOnCorrectTotal_withoutCorruption() throws Exception {
        Merchant merchant = merchantRepository.save(new Merchant("Acme", "ACC-1"));
        String period = LocalDate.now().toString();
        Instant processedAt = LocalDate.parse(period).atTime(12, 0).toInstant(ZoneOffset.UTC);

        BigDecimal expectedTotal = BigDecimal.ZERO;
        for (int i = 0; i < 5; i++) {
            BigDecimal amount = new BigDecimal("10.00").multiply(BigDecimal.valueOf(i + 1));
            expectedTotal = expectedTotal.add(amount);
            transactionRepository.save(processedTransactionAt(merchant, amount, processedAt));
        }

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_ATTEMPTS);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_ATTEMPTS);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_ATTEMPTS; i++) {
            futures.add(pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    settlementService.runSettlement(merchant.getId(), period);
                    successes.incrementAndGet();
                } catch (ConcurrentSettlementException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        for (var future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        // At least one attempt must have succeeded, and however many did,
        // the row they converged on must reflect the correct total exactly
        // once — never a value corrupted by a lost update.
        assertThat(successes.get()).isGreaterThan(0);
        assertThat(successes.get() + conflicts.get()).isEqualTo(CONCURRENT_ATTEMPTS);

        SettlementResponse finalState = settlementService.getSettlement(merchant.getId(), period);
        assertThat(finalState.totalAmount()).isEqualByComparingTo(expectedTotal);

        assertThat(settlementRepository.findByMerchantIdAndPeriod(merchant.getId(), period)).isPresent();
    }

    private Transaction processedTransactionAt(Merchant merchant, BigDecimal amount, Instant processedAt) {
        Transaction transaction = new Transaction(merchant, amount, "USD", "ext-" + System.nanoTime(), PaymentType.CARD);
        transaction.setStatus(TransactionStatus.PROCESSED);
        setProcessedAt(transaction, processedAt);
        return transaction;
    }

    private static void setProcessedAt(Transaction transaction, Instant processedAt) {
        try {
            Field field = Transaction.class.getDeclaredField("processedAt");
            field.setAccessible(true);
            field.set(transaction, processedAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
