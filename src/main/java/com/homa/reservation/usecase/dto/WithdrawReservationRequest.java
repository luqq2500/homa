package com.homa.reservation.usecase.dto;

/** The customer withdraws a Requested reservation (REQ-2). */
public record WithdrawReservationRequest(String reservationId, String customerId) {
}
