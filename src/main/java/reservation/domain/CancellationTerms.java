package reservation.domain;

import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/**
 * What cancelling this booking costs the customer, as the review page and the reservation details show
 * it, for example "Cancel after Fri 10:00: you pay RM18.00. Before that: free".
 *
 * @param freeUntil when the tier's fee starts; on the Flexible tier, the booked start, since it is free
 *     until then
 * @param feeFromThen what the customer pays for cancelling from {@code freeUntil} until the booked start:
 *     the tier's share of the hours fee, which is the full provider price when travel is included
 * @param lockedFrom the booked start: from here the customer cannot cancel until the provider checks in or
 *     the check-in period ends
 * @param lockedUntil the end of the check-in period
 * @param afterCheckIn what cancelling costs once the provider has checked in: it counts as a customer
 *     no-show, so the customer no-show fee
 */
public record CancellationTerms(
        CancellationTier tier, Instant freeUntil, Money feeFromThen, Instant lockedFrom, Instant lockedUntil, Money afterCheckIn) {

    public CancellationTerms {
        Objects.requireNonNull(tier);
        Objects.requireNonNull(freeUntil);
        Objects.requireNonNull(feeFromThen);
        Objects.requireNonNull(lockedFrom);
        Objects.requireNonNull(lockedUntil);
        Objects.requireNonNull(afterCheckIn);
    }

    /** Whether the tier charges anything for cancelling before the booked start. */
    public boolean chargesAFee() {
        return !feeFromThen.isZero();
    }

    /** What cancelling a reserved booking at {@code now}, before the booked start, costs. */
    Money feeAt(Instant now) {
        return now.isBefore(freeUntil) ? Money.ZERO : feeFromThen;
    }
}
