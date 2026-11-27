package com.homa.payment.usecase.dto;

/**
 * The customer taps Pay for a checkout they started (RES-11).
 *
 * @param customerId the acting customer, filled from the token by infra/security
 */
public record PayForReservationRequest(String customerId, String checkoutId) {
}
