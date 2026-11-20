package com.homa.reservation.domain.schedule;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.CheckoutId;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationId;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The taken time of one party: checkout holds and reservation blocks. No two active entries overlap. A checkout
 * hold lasts 15 minutes (RES-11); once it has expired it no longer counts, so no clean-up is needed for
 * correctness. A reservation's block lasts until the reservation is released.
 */
public abstract class Schedule {

    /** RES-11: the provider's time is held for up to 15 minutes while the customer pays. */
    public static final Duration CHECKOUT_HOLD = Duration.ofMinutes(15);

    private static final Object NOBODY = new Object();

    private final List<CalendarEntry> entries = new ArrayList<>();
    private final BookingRefusal refusalWhenTaken;

    protected Schedule(BookingRefusal refusalWhenTaken) {
        this.refusalWhenTaken = refusalWhenTaken;
    }

    /** Whether nothing active at this moment overlaps the interval. */
    public boolean isFree(TimeInterval interval, Instant now) {
        return isFreeExcept(NOBODY, interval, now);
    }

    /** Refuses unless the interval is free, not counting the checkout's own hold. */
    public void requireFreeFor(CheckoutId checkout, TimeInterval interval, Instant now) {
        if (!isFreeExcept(checkout, interval, now)) {
            throw new BookingRefusedException(refusalWhenTaken);
        }
    }

    /** RES-11: holds the interval for the checkout for 15 minutes and returns when the hold expires. */
    public Instant hold(CheckoutId checkout, TimeInterval interval, Instant now) {
        requireFreeFor(checkout, interval, now);
        Instant expiresAt = now.plus(CHECKOUT_HOLD);
        entries.add(new CalendarEntry(checkout, interval, expiresAt));
        return expiresAt;
    }

    /** Whether the checkout's hold is still live: not expired and not released. */
    public boolean isHolding(CheckoutId checkout, Instant now) {
        return entries.stream().anyMatch(entry -> entry.isHeldBy(checkout) && entry.isActiveAt(now));
    }

    /**
     * Turns the checkout's hold into the reservation's block. After the hold expired, the interval must still be
     * free (RES-12).
     */
    public void block(CheckoutId checkout, ReservationId reservation, TimeInterval interval, Instant now) {
        requireFreeFor(checkout, interval, now);
        releaseHold(checkout);
        entries.add(new CalendarEntry(reservation, interval, Instant.MAX));
    }

    /** RES-11: a failed payment releases the hold at once. */
    public void releaseHold(CheckoutId checkout) {
        entries.removeIf(entry -> entry.isHeldBy(checkout));
    }

    /** RES-9: the time opens again once the reservation is Declined, Expired, Cancelled or Completed. */
    public void release(Reservation reservation) {
        ReservationStatus status = reservation.status();
        if (status.holdsCalendar()) {
            throw new ReservationStatusConflictException(status, "release the calendar of");
        }
        entries.removeIf(entry -> entry.isHeldBy(reservation.id()));
    }

    private boolean isFreeExcept(Object ignored, TimeInterval interval, Instant now) {
        return entries.stream()
                .filter(entry -> entry.isActiveAt(now) && !entry.isHeldBy(ignored))
                .noneMatch(entry -> entry.overlaps(interval));
    }
}
