package com.homa.reservation.domain.settlement;

import com.homa.kernel.Money;
import com.homa.reservation.domain.ReservationValues.CustomerCompensation;
import com.homa.reservation.domain.ReservationValues.ProviderFines;
import java.time.Instant;

/**
 * NRS-1 to NRS-3: a reservation without a complete roster when its Check-in Period starts is cancelled. The provider
 * is fined the Base Cancellation Fee + 12% and the customer gets a full refund + 4%. Percentages are of the Total
 * Reservation Fee (CAN-3), and the fine is at most the Total Reservation Fee (CAN-2).
 */
public final class NoRosterPolicy implements SettlementPolicy {

    @Override
    public Settlement settle(SettlementBasis basis, Instant at) {
        ProviderFines fines = basis.values().providerFines();
        CustomerCompensation compensation = basis.values().compensation();
        Money total = basis.quote().total();
        return basis.settlement(basis.zero(), compensation.noRoster().of(total),
                basis.quote().baseCancellationFee().plus(fines.noRoster().of(total)));
    }
}
