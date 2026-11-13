package reservation.usecase.dto;

import java.time.Instant;

public record BookAgainRequest(String customerId, String pastId, Instant start) {}
