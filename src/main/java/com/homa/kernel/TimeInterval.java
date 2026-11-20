package com.homa.kernel;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A half-open period of time, [start, end). The end is strictly after the start. Two intervals that only touch
 * (one ends when the other starts) do not overlap.
 */
public record TimeInterval(Instant start, Instant end) {

    public TimeInterval {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("An interval must end after it starts: " + start + " to " + end);
        }
    }

    public static TimeInterval of(Instant start, Duration duration) {
        return new TimeInterval(start, start.plus(duration));
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    /** True when the two intervals share any moment. */
    public boolean overlaps(TimeInterval other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** True when the instant is at or after the start and before the end. */
    public boolean contains(Instant instant) {
        return !instant.isBefore(start) && instant.isBefore(end);
    }

    /** True when the other interval lies wholly inside this one; the ends may coincide. */
    public boolean contains(TimeInterval other) {
        return !other.start.isBefore(start) && !other.end.isAfter(end);
    }

    /** The same start with the end moved later: the job time plus a buffer (RES-9). */
    public TimeInterval extendEnd(Duration extra) {
        return new TimeInterval(start, end.plus(extra));
    }

    /** The same end with the start moved earlier: the job time plus its check-in period. */
    public TimeInterval extendStart(Duration extra) {
        return new TimeInterval(start.minus(extra), end);
    }
}
