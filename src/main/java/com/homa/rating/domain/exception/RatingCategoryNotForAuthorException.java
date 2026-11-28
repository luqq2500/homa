package com.homa.rating.domain.exception;

import com.homa.rating.domain.PartyRole;
import com.homa.rating.domain.RatingCategory;

/**
 * RTG-7, RTG-8: customers rate providers on Quality of work, Punctuality and Professionalism, and providers rate
 * customers on Accurate request, Access and readiness and Respect.
 */
public class RatingCategoryNotForAuthorException extends RatingRuleViolationException {

    private static final long serialVersionUID = 1L;

    public RatingCategoryNotForAuthorException(RatingCategory category, PartyRole author) {
        super("RTG-7, RTG-8: " + category + " is for " + category.ratedBy() + " ratings, not a " + author + "'s");
    }
}
