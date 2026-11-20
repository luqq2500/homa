package com.homa.provider.domain;

/**
 * Whether customers can book a provider, and if not, why (PRV-6). Closing (PRV-5) and suspension (MON-3, MON-4)
 * join in slice 6.
 */
public enum Bookability {
    BOOKABLE,
    /** The provider has not gone live. */
    NOT_LIVE,
    /** The provider is live but has no active helper. */
    NO_ACTIVE_HELPERS;

    public boolean isBookable() {
        return this == BOOKABLE;
    }
}
