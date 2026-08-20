package com.payrecon.repository;

import com.payrecon.domain.ReconciliationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReconciliationReportRepository extends JpaRepository<ReconciliationReport, Long> {

    List<ReconciliationReport> findByRunDateAndNeedsReviewTrue(LocalDate runDate);

    void deleteByRunDate(LocalDate runDate);

    @Query("select max(r.runDate) from ReconciliationReport r")
    Optional<LocalDate> findLatestRunDate();
}
