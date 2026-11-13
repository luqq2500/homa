package reservation.usecase.dto;

import kernel.Money;

public record EditRequestResponse(String reservationId, Money total, boolean paymentSecured) {}
