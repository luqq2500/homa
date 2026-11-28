package com.homa.rating.domain.exception;

import com.homa.rating.domain.RatingValues;

/** RTG-4: a rating has 1 to 5 stars, and so does each category. */
public class InvalidStarsException extends RatingRuleViolationException {

    private static final long serialVersionUID = 1L;

    public InvalidStarsException(int stars) {
        super("RTG-4: " + stars + " stars is not a rating; stars run from " + RatingValues.MINIMUM_STARS + " to "
                + RatingValues.MAXIMUM_STARS);
    }
}
