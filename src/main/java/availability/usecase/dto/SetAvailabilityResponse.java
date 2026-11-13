package availability.usecase.dto;

import java.util.List;
import kernel.TimeRange;

public record SetAvailabilityResponse(List<TimeRange> openTime) {

    public SetAvailabilityResponse {
        openTime = List.copyOf(openTime);
    }
}
