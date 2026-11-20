package com.homa.provider.domain;

/**
 * Where a provider is in onboarding (PRV-1, PRV-2). Closing and closure (PRV-5) and suspension (MON-3, MON-4) join
 * in slice 6.
 */
public enum ProviderStatus {
    /** The business registration is submitted and its verification is awaited. */
    REGISTERED,
    /** The business verification failed, so the provider can't continue onboarding. */
    VERIFICATION_FAILED,
    /** The business is verified: the provider can add its team and go live. */
    VERIFIED,
    /** The provider is live on Homa. */
    LIVE;

    /** True once the business is verified, before and after going live. */
    public boolean isBusinessVerified() {
        return this == VERIFIED || this == LIVE;
    }
}
