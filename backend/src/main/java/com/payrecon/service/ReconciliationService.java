package com.payrecon.service;

import com.payrecon.dto.ReconciliationResponse;

public interface ReconciliationService {

    ReconciliationResponse reconcile(Long transactionId);
}
