package availability.domain;

import java.time.Instant;

/** A busy time synced from the provider's Google or Apple Calendar. */
public record BusyBlock(Instant start, Instant end) implements TimeRange {

    public BusyBlock {
        TimeRanges.requireEndAfterStart(start, end);
    }

    public static BusyBlock of(Instant start, Instant end) {
        return new BusyBlock(start, end);
    }
}
