package com.homa.customer.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * What Verification found when the customer verified their identity with the document the country policy sets for
 * their status (IDN-1): who they are ({@code personId}), whether they are local (§14), and whether they passed the
 * age check for the minimum age (CUS-10, §13). These are Verification's decisions. Customer only receives them and
 * never reads a date of birth, an identity document number or the minimum age itself.
 */
public record VerifiedIdentity(PersonId personId, boolean local, boolean ageCheckPassed) {

    public VerifiedIdentity {
        Objects.requireNonNull(personId, "personId");
    }

    /**
     * CUS-10, §14: what stops this identity from reserving. In Malaysia only a local may be a customer, and every
     * customer must be aged 18 or above. Empty when the identity allows reserving.
     */
    Optional<ReservationBlocker> reservationBlocker() {
        if (!local) {
            return Optional.of(ReservationBlocker.NOT_LOCAL);
        }
        if (!ageCheckPassed) {
            return Optional.of(ReservationBlocker.UNDER_MINIMUM_AGE);
        }
        return Optional.empty();
    }

    /** CUS-10: true when the identity is local and passed the age check. */
    public boolean allowsReserving() {
        return reservationBlocker().isEmpty();
    }
}
