package com.homa.rating.domain.exception;

import com.homa.rating.domain.RatingSessionStatus;
import com.homa.rating.domain.ReservationId;

/** RTG-3, NSH-6: once revealed, or void after a Provider No Show, a session takes no more ratings. */
public class RatingSessionNotOpenException extends RatingRuleViolationException {

    private static final long serialVersionUID = 1L;

    public RatingSessionNotOpenException(ReservationId reservationId, RatingSessionStatus status) {
        super("RTG-3: the ratings of reservation " + reservationId.value() + " are " + status
                + ", so they take no more ratings");
    }
}
