package com.payrecon.controller;

import com.payrecon.batch.SettlementRunLauncher;
import com.payrecon.dto.SettlementRunResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlement-runs")
public class SettlementRunController {

    private final SettlementRunLauncher launcher;

    public SettlementRunController(SettlementRunLauncher launcher) {
        this.launcher = launcher;
    }

    /** Manually triggers a settlement run for {@code period} (an ISO local date, e.g. "2026-08-19"). */
    @PostMapping
    public ResponseEntity<SettlementRunResult> trigger(@RequestParam String period) {
        return ResponseEntity.ok(launcher.launch(period));
    }
}
