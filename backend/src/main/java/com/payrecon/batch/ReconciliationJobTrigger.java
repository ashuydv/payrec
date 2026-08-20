package com.payrecon.batch;

import com.payrecon.dto.BatchJobRunResponse;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ReconciliationJobTrigger {

    private final JobLauncher jobLauncher;
    private final Job reconciliationJob;

    public ReconciliationJobTrigger(JobLauncher jobLauncher, Job reconciliationJob) {
        this.jobLauncher = jobLauncher;
        this.reconciliationJob = reconciliationJob;
    }

    public BatchJobRunResponse trigger() {
        try {
            var jobParameters = new JobParametersBuilder()
                    // runAt makes every trigger unique, since Spring Batch refuses to
                    // re-run a job with JobParameters identical to a prior execution.
                    .addLong("runAt", Instant.now().toEpochMilli())
                    .toJobParameters();

            var execution = jobLauncher.run(reconciliationJob, jobParameters);
            return new BatchJobRunResponse(execution.getJobId(), execution.getStatus().name());
        } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException
                 | JobRestartException | JobParametersInvalidException e) {
            throw new IllegalStateException("Failed to launch reconciliation job: " + e.getMessage(), e);
        }
    }
}
