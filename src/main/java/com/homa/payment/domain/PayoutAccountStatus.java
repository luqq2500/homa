package com.homa.payment.domain;

/** Where a provider's payout account stands. Only a verified account receives payouts (PRV-2, PRV-10). */
public enum PayoutAccountStatus {
    /** Submitted; Verification is checking that the account is held in the provider's name. Payouts wait. */
    PENDING,
    /** The holder matched: payouts can be sent to this account and the provider can be booked (PRV-2). */
    VERIFIED,
    /** The holder did not match. Payouts wait until the provider submits an account that is verified. */
    NOT_VERIFIED
}
