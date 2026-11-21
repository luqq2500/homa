package com.homa.reservation.domain.schedule;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationId;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import java.util.ArrayList;
import java.util.List;

/**
 * The taken time of one party: the blocks of its Requested and Reserved reservations, no two of which overlap.
 * Nothing is held while a customer pays (P1, RES-11): a block is placed only when a payment is confirmed, so the
 * first confirmed payment for a time wins and any later one is refused.
 *
 * <p>Two payments can be confirmed at nearly the same moment, each against the schedule as it was loaded. The
 * {@link #version()} is the version the schedule was loaded at: whoever saves it first moves it on, and the other
 * save must be refused as a conflict, reload the schedule and ask again, so it then sees the winner's block.
 */
public abstract class Schedule {

    private final List<ScheduleBlock> blocks;
    private final long version;
    private final BookingRefusal refusalWhenTaken;

    protected Schedule(List<ScheduleBlock> blocks, long version, BookingRefusal refusalWhenTaken) {
        this.blocks = new ArrayList<>();
        this.version = version;
        this.refusalWhenTaken = refusalWhenTaken;
        blocks.forEach(this::place);
    }

    /** Whether no block overlaps the interval. */
    public final boolean isFree(TimeInterval interval) {
        return blocks.stream().noneMatch(block -> block.interval().overlaps(interval));
    }

    /** Refuses unless the interval is free. */
    public final void requireFree(TimeInterval interval) {
        if (!isFree(interval)) {
            throw new BookingRefusedException(refusalWhenTaken);
        }
    }

    /** Blocks the interval for a reservation whose payment is confirmed, unless it is already taken. */
    public final void block(ReservationId reservation, TimeInterval interval) {
        place(new ScheduleBlock(reservation, interval));
    }

    /** The time opens again once the reservation is Withdrawn, Declined, Expired, Cancelled or Completed. */
    public void release(Reservation reservation) {
        ReservationStatus status = reservation.status();
        if (status.holdsCalendar()) {
            throw new ReservationStatusConflictException(status, "release the calendar of");
        }
        blocks.removeIf(block -> block.reservation().equals(reservation.id()));
    }

    private void place(ScheduleBlock block) {
        requireFree(block.interval());
        blocks.add(block);
    }

    public List<ScheduleBlock> blocks() {
        return List.copyOf(blocks);
    }

    /** The version this schedule was loaded at, for the optimistic lock that decides which payment saved first. */
    public long version() {
        return version;
    }
}
