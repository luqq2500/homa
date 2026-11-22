package com.homa.provider.domain;

/**
 * Where a provider is in its life on Homa. Closing and closure (PRV-5) and suspension (PRV-12, SUS-5, MON-6) join in
 * slice 6.
 */
public enum ProviderStatus {
    /** The owner has opened the provider account (PRV-1); it builds its team and adds its payout account. */
    OPENED,
    /** The provider has gone live on Homa (PRV-2). */
    LIVE
}
