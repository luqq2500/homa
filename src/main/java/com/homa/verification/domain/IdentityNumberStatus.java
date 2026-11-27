package com.homa.verification.domain;

import com.homa.verification.domain.exception.IdentityNumberAlreadyHeldException;
import java.util.Objects;
import java.util.Optional;

/**
 * What Homa already knows about an identity document number when a person verifies with it. The rule that one
 * number recognises exactly one person (IDN-3), so one verified identity holds one customer account (CUS-10) and
 * one place in one provider (HLP-10), spans every verified person. So the use case looks the number up and passes
 * the answer in; {@link VerifiedPerson#verify} then refuses a number already held by a different person.
 *
 * <p>Hook (DAT-7, slice 6): a {@code BLOCKED} state for a suspended customer's number after erasure, which stays
 * blocked until the suspension ends, like Customer's {@code SignInStatus}.
 */
public final class IdentityNumberStatus {

    private static final IdentityNumberStatus FREE = new IdentityNumberStatus(null);

    private final PersonId holder;

    private IdentityNumberStatus(PersonId holder) {
        this.holder = holder;
    }

    /** No verified person holds the number. */
    public static IdentityNumberStatus free() {
        return FREE;
    }

    /** The number is already verified for {@code holder}. */
    public static IdentityNumberStatus heldBy(PersonId holder) {
        return new IdentityNumberStatus(Objects.requireNonNull(holder, "holder"));
    }

    /** The person who already holds the number. Empty when it is free. */
    public Optional<PersonId> holder() {
        return Optional.ofNullable(holder);
    }

    /** IDN-3: refuses the number for {@code personId} when a different person already holds it. */
    void admit(PersonId personId) {
        if (holder != null && !holder.equals(personId)) {
            throw new IdentityNumberAlreadyHeldException();
        }
    }
}
