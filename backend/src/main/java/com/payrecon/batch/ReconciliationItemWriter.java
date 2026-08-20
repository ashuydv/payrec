package com.payrecon.batch;

import com.payrecon.domain.ReconciliationReport;
import com.payrecon.repository.ReconciliationReportRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class ReconciliationItemWriter implements ItemWriter<ReconciliationReport> {

    private final ReconciliationReportRepository reportRepository;

    public ReconciliationItemWriter(ReconciliationReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public void write(Chunk<? extends ReconciliationReport> chunk) {
        reportRepository.saveAll(chunk.getItems());
    }
}
