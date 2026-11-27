package com.homa.reservation.domain.replica;

import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.ServiceAddress;
import com.homa.reservation.domain.exception.BookingRefusedException;
import java.util.Objects;
import java.util.Optional;

/**
 * A registered customer as Reservation sees it, kept up to date from Customer's events. Only an identity-verified
 * customer can make reservations (CUS-3), and a suspended customer can't (CUS-5). Reservation learns that a customer
 * is verified from Customer, never from Verification.
 *
 * @param identityVerified the customer's identity is verified (CUS-3, CUS-10); a newly registered customer isn't
 *                         yet, and verifies at their first reservation, before paying
 */
public record CustomerStanding(
        String customerId, boolean identityVerified, boolean suspended, ServiceAddress homeAddress) {

    public CustomerStanding {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(homeAddress, "homeAddress");
    }

    /** A customer who has just registered: not yet identity verified, and not suspended (CUS-1, CUS-3). */
    public static CustomerStanding registered(String customerId, ServiceAddress homeAddress) {
        return new CustomerStanding(customerId, false, false, homeAddress);
    }

    /** The same customer once their identity is verified (CUS-3). */
    public CustomerStanding verified() {
        return new CustomerStanding(customerId, true, suspended, homeAddress);
    }

    /** RES-8: the customer moved; new bookings use the new home address, booked reservations keep theirs. */
    public CustomerStanding movedTo(ServiceAddress newHomeAddress) {
        return new CustomerStanding(customerId, identityVerified, suspended, newHomeAddress);
    }

    public boolean canReserve() {
        return refusal().isEmpty();
    }

    /**
     * Why the customer can't reserve, if they can't: a suspension (CUS-5), or an identity not yet verified (CUS-3).
     * A suspension is named first, because verifying wouldn't let a suspended customer reserve.
     */
    public Optional<BookingRefusal> refusal() {
        if (suspended) {
            return Optional.of(BookingRefusal.CUSTOMER_SUSPENDED);
        }
        return identityVerified ? Optional.empty() : Optional.of(BookingRefusal.CUSTOMER_NOT_VERIFIED);
    }

    public void requireCanReserve() {
        refusal().ifPresent(refusal -> {
            throw new BookingRefusedException(refusal);
        });
    }
}
