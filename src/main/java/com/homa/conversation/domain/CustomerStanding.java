package com.homa.conversation.domain;

import com.homa.conversation.domain.exception.SuspendedCustomerCannotStartEnquiryException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A registered customer as Conversation sees them, kept up to date from Customer's events. Only a customer in good
 * standing can start an Enquiry (CHT-1): a registered customer who isn't suspended (CUS-5). Identity verification
 * isn't part of it: browsing and enquiries don't need it, only reserving does (CUS-3), so it is kept as a fact and no
 * rule here reads it.
 *
 * <p>Suspension is held as the period Support decided, so a 7- or 30-day suspension ends by itself when its end
 * passes, with nothing stored changing (CUS-6), and the replica needs no scheduler. {@link #suspended} and
 * {@link #reactivated} are what Customer's suspension events will call once they exist (slice 6): their handlers add
 * no rule of their own.
 */
public final class CustomerStanding {

    private final CustomerId customerId;
    private final boolean identityVerified;
    private final Suspension suspension;

    private CustomerStanding(CustomerId customerId, boolean identityVerified, Suspension suspension) {
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.identityVerified = identityVerified;
        this.suspension = suspension;
    }

    /** A customer who has just registered: not yet identity verified (CUS-3), and not suspended. */
    public static CustomerStanding registered(CustomerId customerId) {
        return new CustomerStanding(customerId, false, null);
    }

    /** Rebuilds a standing from what was stored; {@code suspension} is {@code null} when the customer has none. */
    public static CustomerStanding restore(CustomerId customerId, boolean identityVerified, Suspension suspension) {
        return new CustomerStanding(customerId, identityVerified, suspension);
    }

    /** CUS-3: the customer's identity is verified. */
    public CustomerStanding verified() {
        return new CustomerStanding(customerId, true, suspension);
    }

    /**
     * CUS-4: Support suspended the customer from {@code from}. A 7- or 30-day suspension ends by itself at
     * {@code endsAt}; one until Support lifts it has none ({@code null}), and only {@link #reactivated} ends it
     * (CUS-6).
     */
    public CustomerStanding suspended(Instant from, Instant endsAt) {
        return new CustomerStanding(customerId, identityVerified, new Suspension(from, endsAt));
    }

    /** CUS-6: Support lifted the suspension, or the customer's account is active again. */
    public CustomerStanding reactivated() {
        return new CustomerStanding(customerId, identityVerified, null);
    }

    public boolean isSuspendedAt(Instant now) {
        return suspension != null && suspension.isInForceAt(now);
    }

    /** CHT-1, CUS-5: only a customer who isn't suspended at {@code now} can start an Enquiry. */
    public void requireCanStartEnquiryAt(Instant now) {
        if (isSuspendedAt(now)) {
            throw new SuspendedCustomerCannotStartEnquiryException(customerId.value());
        }
    }

    public CustomerId customerId() {
        return customerId;
    }

    public boolean identityVerified() {
        return identityVerified;
    }

    /** The suspension Support decided, whether or not it is in force yet or any more. Empty when there is none. */
    public Optional<Suspension> suspension() {
        return Optional.ofNullable(suspension);
    }
}
