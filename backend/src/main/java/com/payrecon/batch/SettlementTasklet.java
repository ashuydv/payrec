package com.payrecon.batch;

import com.payrecon.dto.SettlementRunResult;
import com.payrecon.service.SettlementService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Wraps SettlementServiceImpl.runSettlement in a Tasklet rather than a
 * chunk-oriented reader/processor/writer step: the aggregation (group
 * candidates by merchant, sum only MATCHED transactions, upsert one
 * Settlement per merchant) is a single cohesive unit of work, not an
 * item-at-a-time stream, so chunk-based restartability wouldn't buy anything
 * here.
 */
public class SettlementTasklet implements Tasklet {

    public static final String RESULT_KEY = "settlementRunResult";

    private final SettlementService settlementService;

    public SettlementTasklet(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String period = chunkContext.getStepContext().getJobParameters().get("period").toString();
        SettlementRunResult result = settlementService.runSettlement(period);
        chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getExecutionContext().put(RESULT_KEY, result);
        return RepeatStatus.FINISHED;
    }
}
