package com.payrecon.batch;

import com.payrecon.dto.SettlementRunResult;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

/**
 * Launches the settlementJob and blocks for its result. Shared by
 * SettlementRunController (HTTP-triggered runs) and SettlementScheduler
 * (cron-triggered runs) so both go through the exact same launch, status
 * check, and result extraction rather than duplicating it.
 */
@Component
public class SettlementRunLauncher {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    public SettlementRunLauncher(JobLauncher jobLauncher, Job settlementJob) {
        this.jobLauncher = jobLauncher;
        this.settlementJob = settlementJob;
    }

    /**
     * Runs a settlement for {@code period} (an ISO local date, e.g.
     * "2026-08-19") and blocks until it finishes. "startedAt" makes every
     * launch a distinct JobInstance so the same period can be safely
     * re-triggered (a rerun only picks up transactions that weren't settled
     * — or weren't yet MATCHED — last time).
     */
    public SettlementRunResult launch(String period) {
        try {
            JobExecution execution = jobLauncher.run(settlementJob, new JobParametersBuilder()
                    .addString("period", period)
                    .addLong("startedAt", System.currentTimeMillis())
                    .toJobParameters());

            if (execution.getStatus() != BatchStatus.COMPLETED) {
                throw new IllegalStateException(
                        "Settlement run for period %s did not complete: %s".formatted(period, execution.getExitStatus()));
            }

            return (SettlementRunResult) execution.getExecutionContext().get(SettlementTasklet.RESULT_KEY);
        } catch (JobExecutionException e) {
            throw new IllegalStateException("Could not launch settlement run for period " + period, e);
        }
    }
}
