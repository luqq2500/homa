package com.homa.reservation.domain.settlement;

import com.homa.kernel.Money;
import com.homa.reservation.domain.BillableTime;
import java.time.Instant;

/**
 * LMC-1, LMC-2: the customer cancels from the Check-in Period to the job end and pays the provider the Base
 * Cancellation Fee + the hours worked after the first hour + the travel fee. The hours after the first are charged
 * at the discounted hourly cost of all the booked helpers, in 15-minute steps, rounded up.
 */
public final class CustomerLastMinutePolicy implements SettlementPolicy {

    @Override
    public Settlement settle(SettlementBasis basis, Instant at) {
        Money hourlyCost = basis.quote().baseCancellationFee();
        BillableTime afterFirstHour =
                BillableTime.afterFirstHour(basis.jobTime().start(), at, basis.values().partialTimeStep());
        Money fee = hourlyCost.plus(hourlyCost.times(afterFirstHour.inHours())).plus(basis.quote().travelFee());
        return basis.settlement(fee, basis.zero(), basis.zero());
    }
}
