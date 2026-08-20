package com.payrecon.retry.impl;

import com.payrecon.domain.Transaction;
import com.payrecon.domain.TransactionStatus;
import com.payrecon.dto.TransactionResponse;
import com.payrecon.exception.InvalidStatusTransitionException;
import com.payrecon.exception.MaxRetriesExceededException;
import com.payrecon.exception.ResourceNotFoundException;
import com.payrecon.processing.PaymentProcessingResult;
import com.payrecon.processing.PaymentProcessorFactory;
import com.payrecon.repository.TransactionRepository;
import com.payrecon.retry.RetryPolicy;
import com.payrecon.retry.RetryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetryServiceImpl implements RetryService {

    private final TransactionRepository transactionRepository;
    private final PaymentProcessorFactory paymentProcessorFactory;
    private final RetryPolicy retryPolicy;

    public RetryServiceImpl(
            TransactionRepository transactionRepository,
            PaymentProcessorFactory paymentProcessorFactory,
            RetryPolicy retryPolicy) {
        this.transactionRepository = transactionRepository;
        this.paymentProcessorFactory = paymentProcessorFactory;
        this.retryPolicy = retryPolicy;
    }

    @Override
    @Transactional
    public TransactionResponse retry(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.FAILED) {
            throw new InvalidStatusTransitionException(
                    "Only FAILED transactions can be retried, but transaction %d is %s"
                            .formatted(transactionId, transaction.getStatus()));
        }

        if (!retryPolicy.canRetry(transaction.getRetryCount())) {
            throw new MaxRetriesExceededException(
                    "Transaction %d has exhausted its retry budget (%d/%d attempts)"
                            .formatted(transactionId, transaction.getRetryCount(), retryPolicy.maxRetries()));
        }

        PaymentProcessingResult result = paymentProcessorFactory
                .getProcessor(transaction.getPaymentType())
                .process(transaction);

        int attemptNumber = transaction.getRetryCount() + 1;
        transaction.setRetryCount(attemptNumber);

        if (result.success()) {
            transaction.setStatus(TransactionStatus.PROCESSED);
            transaction.setNextRetryAt(null);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setNextRetryAt(retryPolicy.canRetry(attemptNumber)
                    ? retryPolicy.nextRetryAt(attemptNumber)
                    : null);
        }

        return TransactionResponse.from(transaction);
    }
}
