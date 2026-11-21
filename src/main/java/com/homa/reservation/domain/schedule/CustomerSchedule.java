package com.homa.reservation.domain.schedule;

import com.homa.reservation.domain.BookingRefusal;
import java.util.List;
import java.util.Objects;

/**
 * RES-10: a customer can't have two Requested or Reserved reservations with overlapping job times. Blocks the job
 * time only, without check-in or buffer. Aggregate root.
 */
public final class CustomerSchedule extends Schedule {

    private final String customerId;

    /** A customer's schedule with nothing booked yet. */
    public CustomerSchedule(String customerId) {
        this(customerId, List.of(), 0);
    }

    /** A customer's schedule as it was saved, with its blocks and the version it was saved at. */
    public CustomerSchedule(String customerId, List<ScheduleBlock> blocks, long version) {
        super(blocks, version, BookingRefusal.CUSTOMER_BUSY);
        this.customerId = Objects.requireNonNull(customerId, "customerId");
    }

    public String customerId() {
        return customerId;
    }
}
