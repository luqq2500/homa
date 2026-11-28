package com.homa.reservation.domain.settlement;

import com.homa.kernel.Money;
import java.time.Instant;

/**
 * LMC-3: the provider cancels from the Check-in Period to the job end. It is fined the Base Cancellation Fee + 20%
 * and the customer gets a full refund + 7%. Percentages are of the Total Reservation Fee (CAN-3).
 */
public final class ProviderLastMinutePolicy implements SettlementPolicy {

    @Override
    public Settlement settle(SettlementBasis basis, Instant at) {
        Money total = basis.quote().total();
        return basis.settlement(basis.zero(), basis.values().compensation().providerLastMinute().of(total),
                basis.quote().baseCancellationFee().plus(basis.values().providerFines().lastMinute().of(total)));
    }
}
