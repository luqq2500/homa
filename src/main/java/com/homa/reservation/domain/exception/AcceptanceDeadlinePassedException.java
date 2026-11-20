package com.homa.reservation.domain.exception;

import java.time.Instant;

/** ACC-2: a request can be accepted only before its acceptance deadline. */
public final class AcceptanceDeadlinePassedException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public AcceptanceDeadlinePassedException(Instant deadline) {
        super("The acceptance deadline passed at " + deadline);
    }
}
