package com.payrecon.batch;

import com.payrecon.service.SettlementService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * spring.batch.job.enabled=false (application.yml) turns off Spring Boot's
 * default "run every Job bean on startup" behavior — this job runs only when
 * explicitly launched (see SettlementController), since a settlement run is
 * an on-demand/scheduled operation, not something that should fire every
 * time the app restarts.
 */
@Configuration
public class SettlementBatchConfig {

    @Bean
    public Job settlementJob(JobRepository jobRepository, Step settlementStep) {
        return new JobBuilder("settlementJob", jobRepository)
                .start(settlementStep)
                .build();
    }

    @Bean
    public Step settlementStep(
            JobRepository jobRepository, PlatformTransactionManager transactionManager, SettlementService settlementService) {
        return new StepBuilder("settlementStep", jobRepository)
                .tasklet(new SettlementTasklet(settlementService), transactionManager)
                .build();
    }
}
