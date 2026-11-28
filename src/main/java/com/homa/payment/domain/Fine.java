package com.homa.payment.domain;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * One fine on a provider, with the reservation it came from (PAY-4). It starts owing its whole amount and is cleared
 * bit by bit from the provider's payouts (PAY-5). A cleared fine stays on the account as a record (DAT-6).
 *
 * @param amount      the fine as Reservation worked it out, positive and in whole sen
 * @param outstanding what is still owed, from nothing up to the amount
 */
public record Fine(ReservationId reservationId, Money amount, Money outstanding, Instant recordedAt) {

    public Fine {
        Objects.requireNonNull(reservationId, "reservationId");
        Amounts.requirePayable(amount, "A fine");
        Objects.requireNonNull(outstanding, "outstanding");
        Objects.requireNonNull(recordedAt, "recordedAt");
        if (outstanding.isNegative() || outstanding.isGreaterThan(amount)) {
            throw new IllegalArgumentException("A fine's outstanding amount is from nothing up to the fine, was "
                    + outstanding + " of " + amount);
        }
    }

    /** A new fine, owing all of it. */
    static Fine imposed(ReservationId reservationId, Money amount, Instant recordedAt) {
        return new Fine(reservationId, amount, amount, recordedAt);
    }

    /** The same fine with {@code cleared} less to pay; nothing cleared leaves it as it is. */
    Fine clearedBy(Money cleared) {
        return new Fine(reservationId, amount, outstanding.minus(cleared), recordedAt);
    }
}
