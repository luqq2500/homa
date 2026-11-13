package reservation.usecase.dto;

import java.time.Instant;
import java.util.Set;

public record StartEnquiryRequest(String customerId, String providerId, Set<String> jobIds, Instant start, int hours, int helpers) {

    public StartEnquiryRequest {
        jobIds = Set.copyOf(jobIds);
    }
}
