package com.payrecon.repository;

import com.payrecon.domain.ReconciliationReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReconciliationReportRepository extends JpaRepository<ReconciliationReport, Long> {

    List<ReconciliationReport> findByRunDateAndNeedsReviewTrue(LocalDate runDate);

    void deleteByRunDate(LocalDate runDate);
}
