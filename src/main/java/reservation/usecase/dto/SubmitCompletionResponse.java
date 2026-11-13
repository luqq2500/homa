package reservation.usecase.dto;

import java.time.Instant;

public record SubmitCompletionResponse(String reservationId, Instant disputeWindowEndsAt) {}
