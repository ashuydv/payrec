package com.payrecon.batch;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.domain.Merchant;
import com.payrecon.domain.Settlement;
import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import com.payrecon.repository.LedgerEntryRepository;
import com.payrecon.repository.MerchantRepository;
import com.payrecon.repository.SettlementRepository;
import com.payrecon.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check that the settlementJob bean is wired correctly against a
 * real (H2) database: JobRepository/PlatformTransactionManager autoconfig,
 * the Tasklet reading "period" out of JobParameters, and the aggregation
 * actually persisting. The unit tests around SettlementServiceImpl and
 * SettlementMerchantRunner cover the aggregation logic's branches in
 * isolation; this only needs to prove the wiring holds together.
 */
@SpringBootTest
@ActiveProfiles("test")
class SettlementBatchIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job settlementJob;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Test
    void settlementJob_aggregatesAMatchedTransactionIntoANewSettlement() throws Exception {
        Merchant merchant = merchantRepository.save(new Merchant("Acme Co", "ACC-001"));

        Transaction transaction = new Transaction(merchant, new BigDecimal("100.00"), "USD", "ext-batch-1");
        transaction.setStatus(TransactionStatus.PROCESSED);
        transaction = transactionRepository.save(transaction);

        ledgerEntryRepository.save(new LedgerEntry("ext-batch-1", new BigDecimal("100.00"), LedgerSource.INTERNAL));
        ledgerEntryRepository.save(new LedgerEntry("ext-batch-1", new BigDecimal("100.00"), LedgerSource.BANK_FEED));

        String period = LocalDate.now(ZoneOffset.UTC).toString();

        JobExecution execution = jobLauncher.run(settlementJob, new JobParametersBuilder()
                .addString("period", period)
                .addLong("startedAt", System.currentTimeMillis())
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Settlement settlement = settlementRepository.findByMerchantIdAndPeriod(merchant.getId(), period).orElseThrow();
        assertThat(settlement.getTotalAmount()).isEqualByComparingTo("100.00");

        Transaction reloaded = transactionRepository.findById(transaction.getId()).orElseThrow();
        assertThat(reloaded.getSettlement()).isNotNull();
        assertThat(reloaded.getSettlement().getId()).isEqualTo(settlement.getId());
    }
}
