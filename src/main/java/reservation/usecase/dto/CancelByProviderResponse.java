package reservation.usecase.dto;

import kernel.Money;

public record CancelByProviderResponse(String reservationId, Money fine) {}
