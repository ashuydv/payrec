package com.payrecon.reconciliation.impl;

import com.payrecon.domain.Transaction;
import com.payrecon.dto.ReconciliationReportResponse;
import com.payrecon.reconciliation.ReconciliationReportQueryService;
import com.payrecon.repository.ReconciliationReportRepository;
import com.payrecon.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReconciliationReportQueryServiceImpl implements ReconciliationReportQueryService {

    private final ReconciliationReportRepository reportRepository;
    private final TransactionRepository transactionRepository;

    public ReconciliationReportQueryServiceImpl(
            ReconciliationReportRepository reportRepository, TransactionRepository transactionRepository) {
        this.reportRepository = reportRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReconciliationReportResponse> latestNeedsReview() {
        return reportRepository.findLatestRunDate()
                .map(reportRepository::findByRunDateAndNeedsReviewTrue)
                .orElse(List.of())
                .stream()
                .map(report -> ReconciliationReportResponse.from(
                        report,
                        transactionRepository.findByExternalReference(report.getTransactionReference())
                                .map(Transaction::getId)
                                .orElse(null)))
                .toList();
    }
}
