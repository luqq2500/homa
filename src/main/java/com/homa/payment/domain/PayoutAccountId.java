package com.homa.payment.domain;

import java.util.UUID;

/**
 * One payout account a provider submitted: its bank details and holder. Each submission gets its own id, so the
 * holder check Verification sends back names the exact account it checked, and a result for an account the provider
 * has since replaced is recognised as stale (PRV-2, PRV-10). Payment owns it and generates it.
 */
public record PayoutAccountId(String value) {

    public PayoutAccountId {
        Ids.requireText(value, "PayoutAccountId");
    }

    /** A new, unique id. */
    public static PayoutAccountId generate() {
        return new PayoutAccountId(UUID.randomUUID().toString());
    }
}
