package com.payrecon.repository;

import com.payrecon.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByMerchantIdAndPeriod(Long merchantId, String period);
}
