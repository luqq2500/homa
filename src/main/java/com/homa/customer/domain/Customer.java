package com.homa.customer.domain;

import com.homa.customer.domain.exception.CustomerAlreadySuspendedException;
import com.homa.customer.domain.exception.CustomerNotSuspendedException;
import com.homa.customer.domain.exception.MissingSignInException;
import com.homa.customer.domain.exception.TermsOfServiceNotAcceptedException;
import java.time.Instant;
import java.util.Optional;

/**
 * A registered homeowner who makes reservations. A customer exists only through {@link #register}, so only a
 * registered customer can make reservations (CUS-3). Every detail CUS-1 asks for is always present, at registration
 * and after any profile change. Whether the customer is suspended is read from their {@link Suspension} at a given
 * moment, so a suspension for a set period ends on its own (CUS-6).
 */
public final class Customer {

    private final CustomerId id;
    private final SignInId signInId;
    private final TermsAcceptance termsAcceptance;
    private CustomerName name;
    private HomeAddress homeAddress;
    private ArrivalNote arrivalNote;
    private Suspension suspension;

    private Customer(CustomerId id, SignInId signInId, CustomerName name, HomeAddress homeAddress,
            ArrivalNote arrivalNote, TermsAcceptance termsAcceptance) {
        this.id = id;
        this.signInId = signInId;
        this.name = name;
        this.homeAddress = homeAddress;
        this.arrivalNote = arrivalNote;
        this.termsAcceptance = termsAcceptance;
    }

    /**
     * Registers a customer who signed in through an identity provider (CUS-7) with a name, a home address with city
     * and an arrival note (CUS-1), and who has agreed to the Terms of Service (CUS-2). {@code signInStatus} is what
     * Homa already knows about the sign-in: one that already holds an account (CUS-7), or that is blocked after an
     * erasure (CUS-9), is refused. The new customer is active, with a new id, so they start with no ratings and no
     * reliability counts (CUS-7).
     */
    public static Customer register(SignInId signInId, SignInStatus signInStatus, CustomerName name,
            HomeAddress homeAddress, ArrivalNote arrivalNote, TermsAcceptance termsAcceptance) {
        if (signInId == null) {
            throw new MissingSignInException("a sign-in");
        }
        signInStatus.admitNewAccount(signInId);
        Required.detail(name, "a name");
        Required.detail(homeAddress, "a home address");
        Required.detail(arrivalNote, "an arrival note");
        if (termsAcceptance == null) {
            throw new TermsOfServiceNotAcceptedException();
        }
        return new Customer(CustomerId.generate(), signInId, name, homeAddress, arrivalNote, termsAcceptance);
    }

    /** CUS-1: the customer goes by a new name. */
    public void changeName(CustomerName newName) {
        this.name = Required.detail(newName, "a name");
    }

    /**
     * CUS-1: the customer moves home; the new address has a city, which Reservation copies (RES-1). Reservations
     * already Requested keep the address they were made with (RES-8).
     */
    public void changeHomeAddress(HomeAddress newHomeAddress) {
        this.homeAddress = Required.detail(newHomeAddress, "a home address");
    }

    /** CUS-1: the customer tells the team a new way to reach the home. */
    public void changeArrivalNote(ArrivalNote newArrivalNote) {
        this.arrivalNote = Required.detail(newArrivalNote, "an arrival note");
    }

    /**
     * CUS-4: Support suspends the customer at {@code now} for a set period or until Support lifts it, giving the
     * reason. A customer whose suspension is in force can't be suspended again; one whose suspension has ended can.
     */
    public void suspend(SuspensionPeriod period, String reason, Instant now) {
        Suspension newSuspension = Suspension.start(period, reason, now);
        if (isSuspendedAt(now)) {
            throw new CustomerAlreadySuspendedException(id.value());
        }
        this.suspension = newSuspension;
    }

    /**
     * CUS-6: Support confirms the end of a suspension until lifted at {@code now}. A suspension for a set period
     * ends only on its own.
     */
    public void liftSuspension(Instant now) {
        if (!isSuspendedAt(now)) {
            throw new CustomerNotSuspendedException(id.value());
        }
        this.suspension = suspension.liftedAt(now);
    }

    /** CUS-4, CUS-6: true while a suspension is in force at {@code now}. */
    public boolean isSuspendedAt(Instant now) {
        return suspension != null && suspension.isInForceAt(now);
    }

    /** CUS-3: a registered customer can make reservations unless suspended at {@code now} (CUS-5). */
    public boolean canMakeReservationsAt(Instant now) {
        return !isSuspendedAt(now);
    }

    /** The customer's standing at {@code now}. */
    public CustomerStatus statusAt(Instant now) {
        return isSuspendedAt(now) ? CustomerStatus.SUSPENDED : CustomerStatus.ACTIVE;
    }

    public CustomerId id() {
        return id;
    }

    /** The identity-provider account that holds this customer account (CUS-7). */
    public SignInId signInId() {
        return signInId;
    }

    public CustomerName name() {
        return name;
    }

    public HomeAddress homeAddress() {
        return homeAddress;
    }

    public ArrivalNote arrivalNote() {
        return arrivalNote;
    }

    public TermsAcceptance termsAcceptance() {
        return termsAcceptance;
    }

    /** The customer's latest suspension, whether still in force or ended. Empty if never suspended. */
    public Optional<Suspension> suspension() {
        return Optional.ofNullable(suspension);
    }
}
