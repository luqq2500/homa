package policy.domain;

import kernel.Percentage;

/**
 * The cancellation tiers Homa offers. Each provider chooses one. The fee is a share of the hours fee,
 * charged when the customer cancels a reserved booking within the fee window; Homa can change the
 * Standard and Firm fees, and Flexible is always free.
 */
public enum CancellationTier {
    FLEXIBLE(Percentage.ZERO),
    STANDARD(Percentage.of(10)),
    FIRM(Percentage.of(20));

    private final Percentage launchFee;

    CancellationTier(Percentage launchFee) {
        this.launchFee = launchFee;
    }

    /** The share of the hours fee this tier charged at launch, in policy version 1. */
    public Percentage launchFee() {
        return launchFee;
    }
}
