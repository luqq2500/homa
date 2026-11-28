package com.homa.rating.domain.exception;

import com.homa.rating.domain.PartyRole;
import com.homa.rating.domain.ReservationId;

/** RTG-1: the customer rates the provider and the provider rates the customer, once each. */
public class RatingAlreadySubmittedException extends RatingRuleViolationException {

    private static final long serialVersionUID = 1L;

    public RatingAlreadySubmittedException(ReservationId reservationId, PartyRole side) {
        super("RTG-1: the " + side + " already rated reservation " + reservationId.value());
    }
}
