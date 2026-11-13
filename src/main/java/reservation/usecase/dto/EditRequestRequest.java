package reservation.usecase.dto;

import java.time.Instant;
import java.util.Set;

public record EditRequestRequest(String customerId, String reservationId, Set<String> jobIds, Instant start, int hours, int helpers) {

    public EditRequestRequest {
        jobIds = Set.copyOf(jobIds);
    }
}
