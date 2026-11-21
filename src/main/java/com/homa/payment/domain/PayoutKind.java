package com.homa.payment.domain;

/** What a payout pays the provider for. The platform fee applies to both kinds (PAY-3). */
public enum PayoutKind {
    /** The provider's earnings for the job, opened when the reservation is Reserved and owed once it is marked Completed. */
    JOB,
    /** A cancellation, last-minute or no-show fee the customer paid the provider; it needs no completion (PAY-6). */
    FEE
}
