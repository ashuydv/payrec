package com.payrecon.repository;

import com.payrecon.domain.Settlement;
import com.payrecon.domain.SettlementStatus;
import org.springframework.data.jpa.domain.Specification;

public final class SettlementSpecifications {

    private SettlementSpecifications() {
    }

    public static Specification<Settlement> filter(Long merchantId, SettlementStatus status) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (merchantId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("merchant").get("id"), merchantId));
            }
            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            return predicates;
        };
    }
}
