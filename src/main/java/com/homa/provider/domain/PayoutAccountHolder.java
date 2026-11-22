package com.homa.provider.domain;

/**
 * In whose name a provider's verified payout account is held, as Payment reports it after Verification's holder check
 * ({@code PayoutAccountVerifiedIE}). Either one is in the provider's name and counts towards being bookable (PRV-2).
 * One in the business's name is also one of the three facts Business status needs (PRV-7, slice 6).
 */
public enum PayoutAccountHolder {
    /** The account is in the owner's name. */
    OWNER,
    /** The account is in the registered business's name. */
    BUSINESS
}
