package com.homa.reservation.domain.replica;

import com.homa.reservation.domain.QuarterHourDuration;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/** A provider's working hours on one day of the week, for example 8:00–18:00 (OFR-8). */
public record DailyHours(LocalTime opens, LocalTime closes) {

    private static final int START_STEP_MINUTES = 15;

    public DailyHours {
        Objects.requireNonNull(opens, "opens");
        Objects.requireNonNull(closes, "closes");
    }

    /** RES-3: the job time lies within these hours. */
    boolean covers(LocalTime jobStart, LocalTime jobEnd) {
        return !jobStart.isBefore(opens) && !jobEnd.isAfter(closes);
    }

    /** Every job start time, in 15-minute steps from opening, whose job ends by closing. */
    List<LocalTime> startsFor(QuarterHourDuration duration) {
        int length = (int) duration.length().toMinutes();
        int closing = closes.toSecondOfDay() / 60;
        return IntStream.iterate(opens.toSecondOfDay() / 60, start -> start + length <= closing,
                        start -> start + START_STEP_MINUTES)
                .mapToObj(start -> LocalTime.ofSecondOfDay(start * 60L))
                .toList();
    }
}
