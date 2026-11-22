package com.homa.ie.events;

import com.homa.ie.port.PaymentIE;
import java.time.Instant;

/**
 * A provider submitted or changed its payout account. Verification checks the holder against the owner's or the
 * business's identity without the number leaving it (IDN-3); payouts wait until it is verified (PRV-2, PRV-10).
 *
 * @param holder OWNER or BUSINESS
 */
public record PayoutAccountSubmittedIE(
        String eventId, String providerId, String payoutAccountId, String holder, Instant occurredAt)
        implements PaymentIE {
}
