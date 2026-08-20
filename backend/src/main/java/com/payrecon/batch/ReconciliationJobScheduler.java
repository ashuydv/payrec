package com.payrecon.batch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the nightly reconciliation job on a cron schedule. Disabled by
 * default (payrecon.batch.scheduling.enabled=false) so the demo doesn't
 * fire a batch job in the background every time the app starts — enable it
 * to show the "real" scheduled-nightly-job behavior.
 */
@Component
@ConditionalOnProperty(name = "payrecon.batch.scheduling.enabled", havingValue = "true")
public class ReconciliationJobScheduler {

    private final ReconciliationJobTrigger reconciliationJobTrigger;

    public ReconciliationJobScheduler(ReconciliationJobTrigger reconciliationJobTrigger) {
        this.reconciliationJobTrigger = reconciliationJobTrigger;
    }

    @Scheduled(cron = "${payrecon.batch.scheduling.cron:0 0 2 * * *}")
    public void runNightlyReconciliation() {
        reconciliationJobTrigger.trigger();
    }
}
