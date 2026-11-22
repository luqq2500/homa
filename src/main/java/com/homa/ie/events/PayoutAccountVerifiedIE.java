package com.homa.ie.events;

import com.homa.ie.port.PaymentIE;
import java.time.Instant;

/**
 * A provider's payout account was verified in its name, or failed the check. A provider can be booked only with a
 * verified payout account (PRV-2); one in the business's name counts toward Business status (PRV-7).
 *
 * @param holder OWNER or BUSINESS
 */
public record PayoutAccountVerifiedIE(
        String eventId, String providerId, String payoutAccountId, String holder, boolean verified, Instant occurredAt)
        implements PaymentIE {
}
