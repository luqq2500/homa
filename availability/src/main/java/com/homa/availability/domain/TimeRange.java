package com.homa.availability.domain;

import java.time.Instant;

/** A stretch of time from a start to a later end. The end is exclusive, so 10:00 to 13:00 and 13:00 to 14:00 do not overlap. */
public sealed interface TimeRange permits TimeSlot, AvailabilityWindow, BusyBlock {

    Instant start();

    Instant end();

    default boolean overlaps(TimeRange other) {
        return start().isBefore(other.end()) && other.start().isBefore(end());
    }

    /** Whether {@code moment} falls in this range: from the start, up to but not including the end. */
    default boolean includes(Instant moment) {
        return !moment.isBefore(start()) && moment.isBefore(end());
    }
}
