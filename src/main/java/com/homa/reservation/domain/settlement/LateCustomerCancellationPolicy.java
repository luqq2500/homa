package com.homa.reservation.domain.settlement;

import java.time.Instant;

/**
 * CCX-6: after the cut-off the customer pays the provider the Base Cancellation Fee, after discount (CAN-1), and
 * gets the rest of the payment back.
 */
public final class LateCustomerCancellationPolicy implements SettlementPolicy {

    @Override
    public Settlement settle(SettlementBasis basis, Instant at) {
        return basis.settlement(basis.quote().baseCancellationFee(), basis.zero(), basis.zero());
    }
}
