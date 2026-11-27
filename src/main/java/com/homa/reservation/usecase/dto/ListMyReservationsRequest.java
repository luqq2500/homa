package com.homa.reservation.usecase.dto;

/** A customer or a provider lists the reservations they are a party of. */
public record ListMyReservationsRequest(String partyId) {
}
