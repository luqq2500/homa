package com.homa.customer.domain.exception;

import com.homa.customer.domain.SignInId;
import java.time.Instant;
import java.util.Optional;

/**
 * DAT-7: a suspended customer's identity-provider account stays blocked until the suspension ends, even after their
 * data is deleted.
 */
public class SignInBlockedException extends RuntimeException {

    private final transient Optional<Instant> blockedUntil;

    public SignInBlockedException(SignInId signInId, Optional<Instant> blockedUntil) {
        super("DAT-7: this " + signInId.issuer() + " account is blocked until "
                + blockedUntil.map(Instant::toString).orElse("Support lifts its suspension"));
        this.blockedUntil = blockedUntil;
    }

    /** When the sign-in becomes free; empty while it waits for Support to lift the suspension. */
    public Optional<Instant> blockedUntil() {
        return blockedUntil;
    }
}
