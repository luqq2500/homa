package com.homa.availability.domain.exception;

/** Each reservation holds one slot. */
public class ReservationAlreadyHeldException extends AvailabilityRuleViolationException {

    public ReservationAlreadyHeldException(String reservationId) {
        super("Reservation " + reservationId + " already holds a slot");
    }
}
