package com.homa.reservation.domain.settlement;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.reservation.domain.ReservationValues.CustomerCompensation;
import com.homa.reservation.domain.ReservationValues.ProviderFines;
import java.time.Duration;
import java.time.Instant;

/**
 * PCX-1, PCX-2: the provider cancels before the Check-in Period. More than 24 hours before it, the provider is
 * fined the Base Cancellation Fee and the customer is refunded in full. Within 24 hours of it, the provider is fined
 * Base + 10% and the customer gets a full refund + 3%. Percentages are of the Total Reservation Fee (CAN-3).
 */
public final class ProviderCancellationPolicy implements SettlementPolicy {

    @Override
    public Settlement settle(SettlementBasis basis, Instant at) {
        ProviderFines fines = basis.values().providerFines();
        CustomerCompensation compensation = basis.values().compensation();
        boolean within = Duration.between(at, basis.checkInStart()).compareTo(fines.lateCancellationWithin()) <= 0;
        Percentage extraFine = within ? fines.lateCancellation() : Percentage.ZERO;
        Percentage extraCompensation = within ? compensation.lateProviderCancellation() : Percentage.ZERO;
        Money total = basis.quote().total();
        return basis.settlement(basis.zero(), extraCompensation.of(total),
                basis.quote().baseCancellationFee().plus(extraFine.of(total)));
    }
}
