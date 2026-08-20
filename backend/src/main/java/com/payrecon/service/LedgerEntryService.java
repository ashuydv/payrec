package com.payrecon.service;

import com.payrecon.dto.CreateLedgerEntryRequest;
import com.payrecon.dto.LedgerEntryResponse;

import java.util.List;

public interface LedgerEntryService {

    LedgerEntryResponse ingest(CreateLedgerEntryRequest request);

    List<LedgerEntryResponse> listByTransactionReference(String transactionReference);
}
