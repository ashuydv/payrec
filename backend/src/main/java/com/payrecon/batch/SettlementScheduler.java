package com.payrecon.batch;

import com.payrecon.dto.SettlementRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Runs the settlement job automatically once a day, instead of relying on
 * someone to call POST /api/settlement-runs by hand. Settles the *previous*
 * UTC day rather than the current one, on the assumption that a day's
 * transactions have had a full day to arrive and reconcile before their
 * settlement run happens — running "today" mid-day would settle a
 * still-in-progress day and need a rerun anyway (which is safe, just wasteful).
 *
 * Gated behind payrecon.settlement.schedule.enabled so it can be turned off
 * without a code change (and is off in the test profile — see
 * application-test.yml — so test runs never race a cron firing).
 */
@Component
@ConditionalOnProperty(prefix = "payrecon.settlement.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(SettlementScheduler.class);

    private final SettlementRunLauncher launcher;

    public SettlementScheduler(SettlementRunLauncher launcher) {
        this.launcher = launcher;
    }

    @Scheduled(cron = "${payrecon.settlement.schedule.cron:0 0 2 * * *}")
    public void runYesterdaysSettlement() {
        String period = LocalDate.now(ZoneOffset.UTC).minusDays(1).toString();
        try {
            SettlementRunResult result = launcher.launch(period);
            log.info("Scheduled settlement run completed for period {}: {}", period, result);
        } catch (RuntimeException e) {
            // One failed scheduled run shouldn't be fatal — the next day's
            // firing (or a manual POST /api/settlement-runs) can retry it.
            log.error("Scheduled settlement run failed for period {}", period, e);
        }
    }
}
