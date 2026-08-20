package com.payrecon.repository;

import com.payrecon.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long>, JpaSpecificationExecutor<Settlement> {

    Optional<Settlement> findByMerchantIdAndPeriod(Long merchantId, String period);
}
