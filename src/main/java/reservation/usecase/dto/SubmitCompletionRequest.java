package reservation.usecase.dto;

import java.util.List;

public record SubmitCompletionRequest(String providerId, String reservationId, List<String> evidence) {

    public SubmitCompletionRequest {
        evidence = List.copyOf(evidence);
    }
}
