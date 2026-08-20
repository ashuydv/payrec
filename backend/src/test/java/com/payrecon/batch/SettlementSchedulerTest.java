package com.payrecon.batch;

import com.payrecon.dto.SettlementRunResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementSchedulerTest {

    @Mock
    private SettlementRunLauncher launcher;

    @Test
    void runYesterdaysSettlement_launchesForThePreviousUtcDay() {
        SettlementScheduler scheduler = new SettlementScheduler(launcher);
        when(launcher.launch(anyString())).thenReturn(
                new SettlementRunResult("ignored", 0, 0, 0, 0, 0));

        scheduler.runYesterdaysSettlement();

        ArgumentCaptor<String> periodCaptor = ArgumentCaptor.forClass(String.class);
        verify(launcher).launch(periodCaptor.capture());
        assertThat(periodCaptor.getValue()).isEqualTo(LocalDate.now(ZoneOffset.UTC).minusDays(1).toString());
    }

    @Test
    void runYesterdaysSettlement_swallowsException_soOneFailedRunDoesNotCrashTheScheduler() {
        SettlementScheduler scheduler = new SettlementScheduler(launcher);
        when(launcher.launch(anyString())).thenThrow(new IllegalStateException("settlement job failed"));

        assertThatCode(scheduler::runYesterdaysSettlement).doesNotThrowAnyException();
    }
}
