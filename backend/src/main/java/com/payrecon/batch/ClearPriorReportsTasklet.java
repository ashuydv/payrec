package com.payrecon.batch;

import com.payrecon.repository.ReconciliationReportRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDate;

/**
 * Deletes any existing reports for the run date before the reconciliation
 * step writes new ones, so re-running the job for a given day (e.g. an ops
 * user re-triggering it after fixing a data issue) replaces that day's
 * report instead of appending duplicate rows.
 */
public class ClearPriorReportsTasklet implements Tasklet {

    private final ReconciliationReportRepository reportRepository;
    private final LocalDate runDate;

    public ClearPriorReportsTasklet(ReconciliationReportRepository reportRepository, LocalDate runDate) {
        this.reportRepository = reportRepository;
        this.runDate = runDate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        reportRepository.deleteByRunDate(runDate);
        return RepeatStatus.FINISHED;
    }
}
