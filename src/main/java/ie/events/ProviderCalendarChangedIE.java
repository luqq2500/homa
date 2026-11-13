package ie.events;

import ie.port.AvailabilityIE;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import kernel.TimeRange;

/** When a provider can work, as it now stands: their availability without their synced calendar events, and the buffer they keep free around each Homa job. */
public record ProviderCalendarChangedIE(String providerId, List<TimeRange> openTime, Duration buffer, Instant occurredAt) implements AvailabilityIE {

    public ProviderCalendarChangedIE {
        openTime = List.copyOf(openTime);
    }
}
