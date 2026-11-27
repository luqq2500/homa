package com.homa.customer.domain;

import com.homa.customer.domain.exception.IdentityAlreadyHoldsAccountException;
import java.util.Optional;

/**
 * What Homa already knows about a verified identity when it is recorded on a customer account. The rule that one
 * verified identity holds one customer account (CUS-10) spans every customer, so the caller looks the personId up
 * and passes the answer in; {@link Customer#recordIdentityVerified} then refuses an identity that another account
 * already holds. It is the same lookup and refusal as {@link SignInStatus} for a sign-in (CUS-7).
 */
public final class IdentityStatus {

    private static final IdentityStatus FREE = new IdentityStatus(null);

    private final CustomerId holder;

    private IdentityStatus(CustomerId holder) {
        this.holder = holder;
    }

    /** No customer account holds the identity. */
    public static IdentityStatus free() {
        return FREE;
    }

    /** The customer account {@code holder} already holds the identity, whatever that account's state. */
    public static IdentityStatus heldBy(CustomerId holder) {
        return new IdentityStatus(holder);
    }

    /** The customer account that holds the identity. Empty if none does. */
    public Optional<CustomerId> holder() {
        return Optional.ofNullable(holder);
    }

    /**
     * Refuses the identity for {@code customer} when another customer account holds it (CUS-10). The account that
     * already holds it can record it again, for example when the person verifies again.
     */
    void admitFor(CustomerId customer) {
        if (holder != null && !holder.equals(customer)) {
            throw new IdentityAlreadyHoldsAccountException();
        }
    }
}
