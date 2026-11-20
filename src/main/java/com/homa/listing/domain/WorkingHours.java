package com.homa.listing.domain;

import com.homa.listing.domain.exception.InvalidWorkingHoursException;
import java.time.LocalTime;
import java.util.Objects;

/** One working day's hours, such as Kemas Home's 8:00–18:00 (OFR-8). Ends after it starts, on the same day. */
public record WorkingHours(LocalTime start, LocalTime end) {

    public WorkingHours {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new InvalidWorkingHoursException("Working hours must end after they start: " + start + " to " + end);
        }
    }

    /** True when a job from {@code from} to {@code to} on this day falls within these hours; the ends may coincide. */
    public boolean contains(LocalTime from, LocalTime to) {
        return !from.isBefore(start) && !to.isAfter(end);
    }
}
