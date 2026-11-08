package com.homa.policy.domain.exception;

/** No cancellation fee is higher than the customer no-show fee, which is the ceiling. */
public class CancellationFeeAboveNoShowCeilingException extends PolicyRuleViolationException {

    public CancellationFeeAboveNoShowCeilingException(String tier, Object tierFee, Object noShowFee) {
        super("The " + tier + " tier fee of " + tierFee + " of the hours fee is above the customer no-show fee of " + noShowFee);
    }
}
