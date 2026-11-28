package com.homa.rating.domain.exception;

import com.homa.rating.domain.PartyRole;

/** RTG-5: a customer's rating can include media; a provider's rating of a customer is text only. */
public class MediaNotAllowedException extends RatingRuleViolationException {

    private static final long serialVersionUID = 1L;

    public MediaNotAllowedException(PartyRole author) {
        super("RTG-5: a " + author + "'s rating is text only; only a customer's rating can include media");
    }
}
