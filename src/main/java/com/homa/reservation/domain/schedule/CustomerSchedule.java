package com.homa.reservation.domain.schedule;

import com.homa.reservation.domain.BookingRefusal;
import java.util.Objects;

/**
 * RES-10: a customer can't have two Requested or Reserved reservations with overlapping job times. Blocks the job
 * time only, without check-in or buffer. Aggregate root.
 */
public final class CustomerSchedule extends Schedule {

    private final String customerId;

    public CustomerSchedule(String customerId) {
        super(BookingRefusal.CUSTOMER_BUSY);
        this.customerId = Objects.requireNonNull(customerId, "customerId");
    }

    public String customerId() {
        return customerId;
    }
}
