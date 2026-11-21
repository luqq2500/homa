package com.homa.reservation.domain.replica;

import com.homa.reservation.domain.JobDuration;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** A provider's working hours on one day of the week, for example 8:00–18:00 (OFR-8). */
public record DailyHours(LocalTime opens, LocalTime closes) {

    public DailyHours {
        Objects.requireNonNull(opens, "opens");
        Objects.requireNonNull(closes, "closes");
    }

    /** RES-3: the job time lies within these hours. */
    boolean covers(LocalTime jobStart, LocalTime jobEnd) {
        return !jobStart.isBefore(opens) && !jobEnd.isAfter(closes);
    }

    /** Every job start time, in the given steps from opening, whose job ends by closing. */
    List<LocalTime> startsFor(JobDuration duration, Duration step) {
        long length = duration.length().toSeconds();
        long closing = closes.toSecondOfDay();
        return Stream.iterate((long) opens.toSecondOfDay(), start -> start + length <= closing,
                        start -> start + step.toSeconds())
                .map(LocalTime::ofSecondOfDay)
                .toList();
    }
}
