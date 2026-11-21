package com.homa.reservation.domain.schedule;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.ReservationId;
import java.util.Objects;

/** One taken interval of a schedule: a Requested or Reserved reservation's block, kept until it is released. */
public record ScheduleBlock(ReservationId reservation, TimeInterval interval) {

    public ScheduleBlock {
        Objects.requireNonNull(reservation, "reservation");
        Objects.requireNonNull(interval, "interval");
    }
}
