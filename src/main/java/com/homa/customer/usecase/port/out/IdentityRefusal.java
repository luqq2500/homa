package com.homa.customer.usecase.port.out;

import java.time.Instant;

/**
 * A verification Customer could not record on the account that started it, kept so that Support can see why the
 * customer still can't reserve. The event that carried it counts as handled and is not retried, because a retry
 * would be refused the same way.
 *
 * @param eventId    the PersonVerifiedIE that was refused
 * @param customerId the account the verification was started for, as the event named it; null if it named none
 * @param personId   the person Verification verified
 * @param reason     why it was refused
 * @param refusedAt  when Customer refused it
 */
public record IdentityRefusal(String eventId, String customerId, String personId, Reason reason, Instant refusedAt) {

    /** Why a verification could not be recorded. */
    public enum Reason {
        /** CUS-10: another customer account already holds this verified identity. */
        IDENTITY_HOLDS_ANOTHER_ACCOUNT,
        /** CUS-10: this account is already verified as another person. */
        ACCOUNT_HOLDS_ANOTHER_IDENTITY,
        /** The event named no customer account, or one Customer doesn't have. */
        ACCOUNT_NOT_FOUND
    }
}
