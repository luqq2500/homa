package availability.usecase.dto;

import java.util.List;
import kernel.TimeRange;

public record SyncCalendarEventsResponse(List<TimeRange> openTime) {

    public SyncCalendarEventsResponse {
        openTime = List.copyOf(openTime);
    }
}
