package com.payrecon.batch;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.LedgerSource;
import com.payrecon.domain.Merchant;
import com.payrecon.domain.ReconciliationOutcome;
import com.payrecon.domain.ReconciliationReport;
import com.payrecon.domain.Transaction;
import com.payrecon.repository.LedgerEntryRepository;
import com.payrecon.repository.MerchantRepository;
import com.payrecon.repository.ReconciliationReportRepository;
import com.payrecon.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class ReconciliationJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job reconciliationJob;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private ReconciliationReportRepository reportRepository;

    @Test
    void reconciliationJob_producesMatchedMismatchedAndMissingReports() throws Exception {
        jobLauncherTestUtils.setJob(reconciliationJob);

        Merchant merchant = merchantRepository.save(new Merchant("Acme", "ACC-1"));
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        Instant createdAtYesterday = yesterday.atTime(10, 0).toInstant(ZoneOffset.UTC);

        Transaction matched = transactionAt(merchant, "ext-matched", new BigDecimal("100.00"), createdAtYesterday);
        Transaction mismatched = transactionAt(merchant, "ext-mismatched", new BigDecimal("50.00"), createdAtYesterday);
        Transaction missing = transactionAt(merchant, "ext-missing", new BigDecimal("25.00"), createdAtYesterday);
        transactionRepository.saveAll(List.of(matched, mismatched, missing));

        ledgerEntryRepository.saveAll(List.of(
                new LedgerEntry("ext-matched", new BigDecimal("100.00"), LedgerSource.BANK_FEED),
                new LedgerEntry("ext-mismatched", new BigDecimal("45.00"), LedgerSource.BANK_FEED)
        ));

        JobExecution execution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters());

        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        List<ReconciliationReport> reports = reportRepository.findAll();
        assertThat(reports).hasSize(3);

        assertThat(reportFor(reports, "ext-matched").getOutcome()).isEqualTo(ReconciliationOutcome.MATCHED);
        assertThat(reportFor(reports, "ext-matched").isNeedsReview()).isFalse();

        assertThat(reportFor(reports, "ext-mismatched").getOutcome()).isEqualTo(ReconciliationOutcome.MISMATCHED);
        assertThat(reportFor(reports, "ext-mismatched").isNeedsReview()).isTrue();

        assertThat(reportFor(reports, "ext-missing").getOutcome()).isEqualTo(ReconciliationOutcome.MISSING);
        assertThat(reportFor(reports, "ext-missing").isNeedsReview()).isTrue();
    }

    private ReconciliationReport reportFor(List<ReconciliationReport> reports, String reference) {
        return reports.stream()
                .filter(r -> r.getTransactionReference().equals(reference))
                .findFirst()
                .orElseThrow();
    }

    private Transaction transactionAt(Merchant merchant, String reference, BigDecimal amount, Instant createdAt) {
        Transaction transaction = new Transaction(merchant, amount, "USD", reference);
        setCreatedAt(transaction, createdAt);
        return transaction;
    }

    private static void setCreatedAt(Transaction transaction, Instant createdAt) {
        try {
            Field field = Transaction.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(transaction, createdAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
