package availability.usecase.dto;

import java.util.List;
import kernel.TimeRange;

public record SyncCalendarEventsRequest(String providerId, List<TimeRange> events) {

    public SyncCalendarEventsRequest {
        events = List.copyOf(events);
    }
}
