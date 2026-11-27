package com.homa.payment.usecase.dto;

import java.util.List;

/**
 * What the run released.
 *
 * @param releasedPayoutIds          payouts released and sent
 * @param waitingForPayoutAccountIds payouts that were due but wait, because their provider has no verified payout
 *                                   account (PRV-2, PRV-10)
 */
public record ReleaseDuePayoutsResponse(List<String> releasedPayoutIds, List<String> waitingForPayoutAccountIds) {

    public ReleaseDuePayoutsResponse {
        releasedPayoutIds = List.copyOf(releasedPayoutIds);
        waitingForPayoutAccountIds = List.copyOf(waitingForPayoutAccountIds);
    }
}
