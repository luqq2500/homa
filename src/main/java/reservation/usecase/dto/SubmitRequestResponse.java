package reservation.usecase.dto;

import java.time.Instant;
import kernel.Money;

public record SubmitRequestResponse(String reservationId, Money providerPrice, Money serviceFee, Money total, boolean supervisorIncluded, Instant expiresAt) {}
