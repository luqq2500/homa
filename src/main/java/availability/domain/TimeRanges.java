package availability.domain;

import availability.domain.exception.TimeRangeEndsBeforeItStartsException;
import java.time.Instant;

/** The rule every time range shares: it ends after it starts. */
final class TimeRanges {

    private TimeRanges() {
    }

    static void requireEndAfterStart(Instant start, Instant end) {
        if (!end.isAfter(start)) {
            throw new TimeRangeEndsBeforeItStartsException(start, end);
        }
    }
}
