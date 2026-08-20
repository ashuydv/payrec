package com.payrecon.batch;

import com.payrecon.domain.ReconciliationReport;
import com.payrecon.domain.Transaction;
import com.payrecon.reconciliation.rule.ReconciliationRule;
import com.payrecon.repository.LedgerEntryRepository;
import com.payrecon.repository.ReconciliationReportRepository;
import com.payrecon.repository.TransactionRepository;
import com.payrecon.repository.TransactionSpecifications;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Nightly reconciliation batch job: reads yesterday's transactions in
 * chunks, reconciles each against the bank feed ledger, and writes a
 * ReconciliationReport row per transaction — flagging MISMATCHED/MISSING
 * ones for ops review.
 *
 * Mid-batch failure handling: a single bad record (e.g. a null-amount
 * transaction blowing up the rule's amount comparison) is caught by the
 * step's skip policy — up to SKIP_LIMIT such records are logged and skipped
 * without failing the whole job, so one corrupt row can't block reconciling
 * the other 9,999. Exceeding the skip limit still fails the job, on the
 * theory that a job skipping hundreds of records signals a systemic problem
 * (e.g. the bank feed didn't load) that ops should see, not silently paper
 * over.
 *
 * The job's first step (clearPriorReportsStep) deletes any existing reports
 * for the run date before reconciling, so re-triggering the job for a day
 * that already has a report (e.g. an ops user re-running it after fixing a
 * data issue) replaces that day's report instead of appending duplicates.
 */
@Configuration
public class ReconciliationJobConfig {

    private static final int CHUNK_SIZE = 50;
    private static final int SKIP_LIMIT = 20;

    @Bean
    public Job reconciliationJob(
            JobRepository jobRepository,
            Step clearPriorReportsStep,
            Step reconciliationStep) {
        return new JobBuilder("reconciliationJob", jobRepository)
                .start(clearPriorReportsStep)
                .next(reconciliationStep)
                .build();
    }

    @Bean
    public Step clearPriorReportsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ReconciliationReportRepository reportRepository) {

        LocalDate runDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);

        return new StepBuilder("clearPriorReportsStep", jobRepository)
                .tasklet(new ClearPriorReportsTasklet(reportRepository, runDate), transactionManager)
                .build();
    }

    @Bean
    public Step reconciliationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TransactionRepository transactionRepository,
            LedgerEntryRepository ledgerEntryRepository,
            ReconciliationReportRepository reportRepository,
            @Qualifier("exactMatchRule") ReconciliationRule reconciliationRule) {

        LocalDate runDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);

        return new StepBuilder("reconciliationStep", jobRepository)
                .<Transaction, ReconciliationReport>chunk(CHUNK_SIZE, transactionManager)
                .reader(yesterdaysTransactionsReader(transactionRepository, runDate))
                .processor(new ReconciliationItemProcessor(ledgerEntryRepository, reconciliationRule, runDate))
                .writer(new ReconciliationItemWriter(reportRepository))
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .build();
    }

    private RepositoryItemReader<Transaction> yesterdaysTransactionsReader(
            TransactionRepository transactionRepository, LocalDate runDate) {

        Instant from = runDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = runDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

        return new RepositoryItemReaderBuilder<Transaction>()
                .name("yesterdaysTransactionsReader")
                .repository(transactionRepository)
                .methodName("findAll")
                .arguments(TransactionSpecifications.filter(null, null, from, to))
                .pageSize(CHUNK_SIZE)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
    }
}
