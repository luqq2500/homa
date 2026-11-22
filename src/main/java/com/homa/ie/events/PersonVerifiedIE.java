package com.homa.ie.events;

import com.homa.ie.port.VerificationIE;
import java.time.Instant;

/**
 * A person passed identity verification with the document the country policy sets for their status (IDN-1).
 * Customer uses it for CUS-3 and CUS-10; Provider for owners (PRV-1) and members (HLP-1 to HLP-3). One person keeps
 * one personId across roles. The identity document number never leaves Verification (IDN-3).
 *
 * @param role           CUSTOMER, OWNER or MEMBER: the role the verification was started for
 * @param status         LOCAL or FOREIGN (§14)
 * @param ageCheckPassed true when the person is aged 18 or above (CUS-10, PRV-1, HLP-1)
 * @param permitExpiry   for a foreign person, the work permit's expiry as an ISO date (yyyy-MM-dd); null for local
 * @param nextDueAt      when re-verification falls due (MON-1); null for a customer, who doesn't re-verify
 */
public record PersonVerifiedIE(
        String eventId,
        String personId,
        String role,
        String status,
        boolean ageCheckPassed,
        String permitExpiry,
        Instant nextDueAt,
        Instant occurredAt) implements VerificationIE {
}
