package availability.domain;

import availability.domain.exception.InvalidBufferException;
import java.time.Duration;
import java.util.List;
import kernel.TimeRange;

/**
 * When a provider can work: the availability they set, the events synced from their own calendar,
 * and the buffer they keep free around each accepted Homa job. The buffer applies around Homa jobs
 * only, not around synced events, and changing it does not affect bookings already accepted.
 */
public final class ProviderCalendar {

    private final String providerId;
    private List<TimeRange> slots;
    private List<TimeRange> events;
    private Duration buffer;

    public ProviderCalendar(String providerId, List<TimeRange> slots, List<TimeRange> events, Duration buffer) {
        this.providerId = providerId;
        this.slots = TimeRange.merge(slots);
        this.events = TimeRange.merge(events);
        this.buffer = checked(buffer);
    }

    /** A calendar with no availability yet, no synced events and no buffer. */
    public static ProviderCalendar startFor(String providerId) {
        return new ProviderCalendar(providerId, List.of(), List.of(), Duration.ZERO);
    }

    /** Availability times that run back to back count as one stretch. */
    public void setAvailability(List<TimeRange> slots) {
        this.slots = TimeRange.merge(slots);
    }

    public void syncEvents(List<TimeRange> events) {
        this.events = TimeRange.merge(events);
    }

    public void setBuffer(Duration buffer) {
        this.buffer = checked(buffer);
    }

    /** The availability with the synced events taken out. An event that ends exactly when a slot starts does not block it. */
    public List<TimeRange> openTime() {
        return TimeRange.subtract(slots, events);
    }

    private static Duration checked(Duration buffer) {
        if (buffer == null || buffer.isNegative()) {
            throw new InvalidBufferException();
        }
        return buffer;
    }

    public String providerId() {
        return providerId;
    }

    public List<TimeRange> slots() {
        return slots;
    }

    public List<TimeRange> events() {
        return events;
    }

    public Duration buffer() {
        return buffer;
    }
}
