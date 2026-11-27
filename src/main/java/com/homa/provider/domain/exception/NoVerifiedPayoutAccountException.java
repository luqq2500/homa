package com.homa.provider.domain.exception;

/** PRV-2: a provider must have a verified payout account in its name to go live. */
public final class NoVerifiedPayoutAccountException extends ProviderRuleViolationException {

    private static final long serialVersionUID = 1L;

    public NoVerifiedPayoutAccountException(String providerId) {
        super("Provider " + providerId + " has no verified payout account in its name (PRV-2)");
    }
}
