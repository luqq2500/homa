package com.homa.availability.domain;

import java.time.Instant;

/** A stretch of time the provider says they can work. */
public record AvailabilityWindow(Instant start, Instant end) implements TimeRange {

    public AvailabilityWindow {
        TimeRanges.requireEndAfterStart(start, end);
    }

    public static AvailabilityWindow of(Instant start, Instant end) {
        return new AvailabilityWindow(start, end);
    }
}
