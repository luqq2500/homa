package availability.usecase.dto;

import java.time.Duration;
import java.util.List;
import kernel.TimeRange;

public record ViewCalendarResponse(List<TimeRange> slots, List<TimeRange> events, Duration buffer, List<TimeRange> openTime) {

    public ViewCalendarResponse {
        slots = List.copyOf(slots);
        events = List.copyOf(events);
        openTime = List.copyOf(openTime);
    }
}
