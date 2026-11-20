package com.homa.payment.domain;

/** What a payout pays the provider for. The platform fee applies only to a JOB payout (PAY-3). */
public enum PayoutKind {
    /** The provider's earnings for a job marked Completed. */
    JOB,
    /** A cancellation, last-minute or no-show fee the customer paid the provider (PAY-6). */
    FEE
}
