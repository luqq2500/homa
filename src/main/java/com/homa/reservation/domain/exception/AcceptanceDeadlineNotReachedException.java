package com.homa.reservation.domain.exception;

import java.time.Instant;

/** REQ-4: a request expires only once its acceptance deadline is reached; until then the provider can accept it. */
public final class AcceptanceDeadlineNotReachedException extends ReservationDomainException {

    private static final long serialVersionUID = 1L;

    public AcceptanceDeadlineNotReachedException(Instant deadline) {
        super("The request can expire from its acceptance deadline at " + deadline);
    }
}
