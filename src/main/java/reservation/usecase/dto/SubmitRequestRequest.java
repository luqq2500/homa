package reservation.usecase.dto;

import java.time.Instant;
import java.util.Set;

public record SubmitRequestRequest(String customerId, String providerId, String enquiryId, String paymentMethod, Set<String> jobIds, Instant start, int hours, int helpers) {

    public SubmitRequestRequest {
        jobIds = Set.copyOf(jobIds);
    }
}
