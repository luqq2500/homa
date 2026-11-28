package com.homa.rating.domain.exception;

import com.homa.rating.domain.RatingId;
import com.homa.rating.domain.ReservationId;

/** RTG-11: Support removes a rating that stands in the reservation's session. */
public class RatingNotFoundException extends RatingRuleViolationException {

    private static final long serialVersionUID = 1L;

    public RatingNotFoundException(ReservationId reservationId, RatingId ratingId) {
        super("RTG-11: reservation " + reservationId.value() + " has no rating " + ratingId.value() + " to remove");
    }
}
