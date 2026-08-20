package com.payrecon.settlement;

import com.payrecon.dto.SettlementResponse;

public interface SettlementService {

    /**
     * (Re-)computes the settlement total for a merchant/period by summing
     * PROCESSED transactions in that period, creating the Settlement row if
     * it doesn't exist yet or updating it in place otherwise. Safe under
     * concurrent calls for the same merchant/period — see
     * SettlementServiceImpl for how.
     */
    SettlementResponse runSettlement(Long merchantId, String period);

    SettlementResponse getSettlement(Long merchantId, String period);
}
