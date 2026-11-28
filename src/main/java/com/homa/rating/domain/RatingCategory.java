package com.homa.rating.domain;

import java.util.Arrays;
import java.util.List;

/**
 * The stars a rating can give besides its overall stars, three per side. Customers rate providers on Quality of
 * work, Punctuality and Professionalism (RTG-7); providers rate customers on Accurate request, Access and readiness
 * and Respect (RTG-8).
 */
public enum RatingCategory {
    QUALITY_OF_WORK(PartyRole.CUSTOMER),
    PUNCTUALITY(PartyRole.CUSTOMER),
    PROFESSIONALISM(PartyRole.CUSTOMER),
    ACCURATE_REQUEST(PartyRole.PROVIDER),
    ACCESS_AND_READINESS(PartyRole.PROVIDER),
    RESPECT(PartyRole.PROVIDER);

    private final PartyRole ratedBy;

    RatingCategory(PartyRole ratedBy) {
        this.ratedBy = ratedBy;
    }

    /** The side that gives stars on this category. */
    public PartyRole ratedBy() {
        return ratedBy;
    }

    /** The categories a rating written by this side can give stars on (RTG-7, RTG-8). */
    public static List<RatingCategory> of(PartyRole author) {
        return Arrays.stream(values()).filter(category -> category.ratedBy == author).toList();
    }
}
