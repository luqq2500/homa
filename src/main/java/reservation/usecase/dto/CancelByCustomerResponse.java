package reservation.usecase.dto;

import kernel.Money;

public record CancelByCustomerResponse(String reservationId, Money providerReceives) {}
