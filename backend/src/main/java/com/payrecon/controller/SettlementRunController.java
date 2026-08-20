package com.payrecon.controller;

import com.payrecon.batch.SettlementTasklet;
import com.payrecon.dto.SettlementRunResult;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlement-runs")
public class SettlementRunController {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    public SettlementRunController(JobLauncher jobLauncher, Job settlementJob) {
        this.jobLauncher = jobLauncher;
        this.settlementJob = settlementJob;
    }

    /**
     * Triggers a settlement run for {@code period} (an ISO local date, e.g.
     * "2026-08-19") and blocks until it finishes. "startedAt" makes every
     * launch a distinct JobInstance so the same period can be safely
     * re-triggered (a rerun only picks up transactions that weren't settled
     * — or weren't yet MATCHED — last time).
     */
    @PostMapping
    public ResponseEntity<SettlementRunResult> trigger(@RequestParam String period) {
        try {
            JobExecution execution = jobLauncher.run(settlementJob, new JobParametersBuilder()
                    .addString("period", period)
                    .addLong("startedAt", System.currentTimeMillis())
                    .toJobParameters());

            if (execution.getStatus() != BatchStatus.COMPLETED) {
                throw new IllegalStateException(
                        "Settlement run for period %s did not complete: %s".formatted(period, execution.getExitStatus()));
            }

            SettlementRunResult result =
                    (SettlementRunResult) execution.getExecutionContext().get(SettlementTasklet.RESULT_KEY);
            return ResponseEntity.ok(result);
        } catch (JobExecutionException e) {
            throw new IllegalStateException("Could not launch settlement run for period " + period, e);
        }
    }
}
