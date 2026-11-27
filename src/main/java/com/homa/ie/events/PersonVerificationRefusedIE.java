package com.homa.ie.events;

import com.homa.ie.port.VerificationIE;
import java.time.Instant;

/**
 * Verification refused a person's verification (IDN-1 to IDN-3, CUS-10, PRV-1, HLP-1 to HLP-3). Verification never
 * stores a person it refused, so this, not {@code PersonVerifiedIE}, is how Customer learns why a customer can't
 * reserve: Aisyah's 16-year-old son is under the minimum age, a foreign visitor has a status the country policy
 * doesn't allow for customers, and Daniel's second account names an identity another person already holds. Provider
 * ignores it for now. Never the identity number, nor any message that could hold it (IDN-3).
 *
 * @param role         CUSTOMER, OWNER or MEMBER: the role the verification was started for
 * @param accountId    for role CUSTOMER, the customer account that started it (CUS-3); null for OWNER and MEMBER
 * @param reason       UNDER_MINIMUM_AGE, STATUS_NOT_ALLOWED_FOR_ROLE, IDENTITY_ALREADY_HELD, DOCUMENT_NOT_ACCEPTED or
 *                     CHECK_NOT_PASSED
 * @param refusedUnder the rule that refused it, such as "CUS-10" or "IDN-3"
 */
public record PersonVerificationRefusedIE(
        String eventId,
        String personId,
        String role,
        String accountId,
        String reason,
        String refusedUnder,
        Instant occurredAt) implements VerificationIE {
}
