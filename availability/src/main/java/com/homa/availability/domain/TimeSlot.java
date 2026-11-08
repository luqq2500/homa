package com.homa.availability.domain;

import com.homa.availability.domain.exception.SlotStartNotOnQuarterHourException;
import java.time.Duration;
import java.time.Instant;

/** The time a booking would take: from a start on a 15-minute step to its end. */
public record TimeSlot(Instant start, Instant end) implements TimeRange {

    static final Duration STEP = Duration.ofMinutes(15);

    public TimeSlot {
        TimeRanges.requireEndAfterStart(start, end);
        if (start.getEpochSecond() % STEP.toSeconds() != 0 || start.getNano() != 0) {
            throw new SlotStartNotOnQuarterHourException(start);
        }
    }

    public static TimeSlot of(Instant start, Duration length) {
        return new TimeSlot(start, start.plus(length));
    }

    public Duration length() {
        return Duration.between(start, end);
    }

    /** The same length of time, starting at {@code newStart}. */
    TimeSlot startingAt(Instant newStart) {
        return of(newStart, length());
    }
}
