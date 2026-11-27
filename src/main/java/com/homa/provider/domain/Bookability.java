package com.homa.provider.domain;

/**
 * Whether customers can book a provider, and if not, why (PRV-2). The reasons other than {@link #NOT_LIVE} are the
 * ones {@code ProviderBookabilityChangedIE} carries for a live provider, under the same names. SUSPENDED (PRV-12,
 * SUS-5) and CLOSING (PRV-5) join in slice 6.
 */
public enum Bookability {
    BOOKABLE,
    /** The provider has not gone live. */
    NOT_LIVE,
    /** The provider has no verified payout account in its name. */
    NO_VERIFIED_PAYOUT_ACCOUNT,
    /** The provider has no active helper. */
    NO_ACTIVE_HELPERS;

    public boolean isBookable() {
        return this == BOOKABLE;
    }
}
