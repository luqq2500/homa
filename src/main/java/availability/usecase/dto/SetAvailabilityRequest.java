package availability.usecase.dto;

import java.util.List;
import kernel.TimeRange;

public record SetAvailabilityRequest(String providerId, List<TimeRange> slots) {

    public SetAvailabilityRequest {
        slots = List.copyOf(slots);
    }
}
