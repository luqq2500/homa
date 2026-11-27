package com.homa.reservation.usecase.dto;

/** The price of a booking with the provider, at the customer's home address (RES-6, OFR-4).
 *
 * @param minutes the job's length in minutes */
public record QuoteReservationRequest(String customerId, String providerId, int minutes, int helpers) {
}
