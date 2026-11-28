package com.homa.reservation.usecase.dto;

/** The customer asks what cancelling a Reserved reservation would cost now (CCX), before confirming. */
public record PreviewCustomerCancellationRequest(String reservationId, String customerId) {
}
