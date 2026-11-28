package com.homa.rating.domain.exception;

import com.homa.rating.domain.Party;
import com.homa.rating.domain.ReservationId;

/** RTG-1: only the reservation's own customer and provider rate each other. */
public class RatingAuthorNotAPartyException extends RatingRuleViolationException {

    private static final long serialVersionUID = 1L;

    public RatingAuthorNotAPartyException(ReservationId reservationId, Party author) {
        super("RTG-1: " + author.role() + " " + author.id() + " is not a party of reservation "
                + reservationId.value());
    }
}
