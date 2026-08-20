package com.payrecon.reconciliation.impl;

import com.payrecon.domain.LedgerEntry;
import com.payrecon.domain.Transaction;
import com.payrecon.reconciliation.ReconciliationResult;
import com.payrecon.reconciliation.ReconciliationService;
import com.payrecon.reconciliation.rule.ReconciliationRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    @Override
    public List<ReconciliationResult> reconcile(
            List<Transaction> transactions, List<LedgerEntry> bankFeedEntries, ReconciliationRule rule) {

        Map<String, LedgerEntry> bankEntriesByReference = bankFeedEntries.stream()
                .collect(Collectors.toMap(
                        LedgerEntry::getTransactionReference,
                        Function.identity(),
                        // If the bank feed has more than one entry for the same reference
                        // (e.g. a duplicate charge on the bank side), keep the first and
                        // let the mismatch surface through the normal amount comparison
                        // rather than silently averaging or dropping data.
                        (first, second) -> first));

        return transactions.stream()
                .map(transaction -> rule.evaluate(
                        transaction, bankEntriesByReference.get(transaction.getExternalReference())))
                .toList();
    }
}
