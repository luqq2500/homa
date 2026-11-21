package com.homa.customer.domain;

import com.homa.customer.domain.exception.SignInAlreadyHoldsAccountException;
import com.homa.customer.domain.exception.SignInBlockedException;
import java.time.Instant;
import java.util.Optional;

/**
 * What Homa already knows about a sign-in when someone registers with it. The rule that one identity-provider
 * account holds only one Homa customer account (CUS-7) spans every customer, so the caller looks the sign-in up and
 * passes the answer in; {@link Customer#register} then refuses a sign-in that isn't free.
 */
public final class SignInStatus {

    /** Whether the sign-in can hold a new account. */
    public enum State {
        /** No customer account holds the sign-in. */
        FREE,
        /** A customer account already holds the sign-in (CUS-7), whatever that account's state. */
        TAKEN,
        /** The sign-in belonged to an erased customer whose suspension hasn't ended yet (CUS-9). */
        BLOCKED
    }

    private static final SignInStatus FREE = new SignInStatus(State.FREE, null);
    private static final SignInStatus TAKEN = new SignInStatus(State.TAKEN, null);

    private final State state;
    private final Instant blockedUntil;

    private SignInStatus(State state, Instant blockedUntil) {
        this.state = state;
        this.blockedUntil = blockedUntil;
    }

    /** No customer account holds the sign-in. */
    public static SignInStatus free() {
        return FREE;
    }

    /** A customer account already holds the sign-in (CUS-7). */
    public static SignInStatus taken() {
        return TAKEN;
    }

    /**
     * CUS-9: the sign-in of an erased customer who was suspended stays blocked while that suspension is in force at
     * {@code now}: until it runs out, or until Support lifts one until lifted. After that the sign-in is free.
     */
    public static SignInStatus blockedWhile(Suspension suspension, Instant now) {
        return suspension.isInForceAt(now) ? new SignInStatus(State.BLOCKED, suspension.endsAt().orElse(null)) : FREE;
    }

    public State state() {
        return state;
    }

    /** When a blocked sign-in becomes free. Empty while it waits for Support to lift the suspension, or if not blocked. */
    public Optional<Instant> blockedUntil() {
        return Optional.ofNullable(blockedUntil);
    }

    /** Refuses a new account for {@code signInId} unless the sign-in is free (CUS-7, CUS-9). */
    void admitNewAccount(SignInId signInId) {
        if (state == State.TAKEN) {
            throw new SignInAlreadyHoldsAccountException(signInId);
        }
        if (state == State.BLOCKED) {
            throw new SignInBlockedException(signInId, blockedUntil());
        }
    }
}
