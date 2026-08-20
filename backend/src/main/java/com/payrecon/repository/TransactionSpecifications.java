package com.payrecon.repository;

import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> filter(TransactionStatus status, Long merchantId, Instant from, Instant to) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (status != null) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }
            if (merchantId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("merchant").get("id"), merchantId));
            }
            if (from != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return predicates;
        };
    }
}
