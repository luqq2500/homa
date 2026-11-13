package reservation.domain;

import java.time.Duration;
import java.util.List;
import kernel.TimeRange;

/** When a provider can work, from their calendar: the time left open and the buffer they keep between jobs. */
public record ProviderOpenTime(String providerId, List<TimeRange> openTime, Duration buffer) {

    public ProviderOpenTime {
        openTime = List.copyOf(openTime);
    }

    /** A provider who has set no availability yet. */
    public static ProviderOpenTime none(String providerId) {
        return new ProviderOpenTime(providerId, List.of(), Duration.ZERO);
    }

    /** The provider's schedule, with the booked times of the reservations they have accepted. */
    public ProviderSchedule scheduleWith(List<TimeRange> acceptedJobs) {
        return new ProviderSchedule(openTime, buffer, acceptedJobs);
    }
}
