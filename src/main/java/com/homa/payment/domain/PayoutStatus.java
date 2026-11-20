package com.homa.payment.domain;

/** Where a provider's payout stands. */
public enum PayoutStatus {
    /** Waiting for the Protection Window to end and every report to be resolved (PAY-2). */
    PENDING,
    /** Sent to the provider, after the platform fee (PAY-3). */
    RELEASED,
    /** Will never be released: the job turned out to be a Provider No Show after it was marked Completed (NSH-6). */
    VOID
}
