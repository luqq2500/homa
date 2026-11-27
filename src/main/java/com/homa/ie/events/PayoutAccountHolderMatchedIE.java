package com.homa.ie.events;

import com.homa.ie.port.VerificationIE;
import java.time.Instant;

/**
 * Verification checked a submitted payout account's holder against the owner's verified identity, or the business's
 * registered name, through the payment provider's account-holder check. Only the outcome leaves Verification; the
 * identity number never does (IDN-3). Payment marks the account verified or not (PRV-2, PRV-7, PRV-10).
 *
 * @param holder OWNER or BUSINESS
 */
public record PayoutAccountHolderMatchedIE(
        String eventId, String payoutAccountId, String providerId, String holder, boolean matched, Instant occurredAt)
        implements VerificationIE {
}
