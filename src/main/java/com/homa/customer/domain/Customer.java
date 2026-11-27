package com.homa.customer.domain;

import com.homa.customer.domain.exception.AccountHoldsAnotherIdentityException;
import com.homa.customer.domain.exception.CustomerAlreadySuspendedException;
import com.homa.customer.domain.exception.CustomerNotSuspendedException;
import com.homa.customer.domain.exception.MissingSignInException;
import com.homa.customer.domain.exception.TermsOfServiceNotAcceptedException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A registered homeowner who makes reservations. A customer exists only through {@link #register}, and every detail
 * CUS-1 asks for is always present, at registration and after any profile change. A new customer is unverified:
 * browsing and enquiries don't need verification, but reserving does, so they verify at their first reservation,
 * before paying (CUS-3). Only a verified identity that is local and aged 18 or above lets them reserve, and one
 * verified identity holds one customer account (CUS-10). Whether the customer is suspended is read from the
 * {@link Suspension} Support decided, at a given moment, so a 7- or 30-day suspension ends on its own (CUS-6).
 *
 * <p>Room is left for later slices: an optional, unverified profile photo on the profile that never blocks a
 * reservation (IDN-4); the customer's own {@code PrivacyConsent}, recorded at registration (DAT-11, slice 9); and
 * deactivation and erasure (CUS-8, CUS-9, slice 6). Blocking a suspended customer's identity number is
 * Verification's (DAT-7); this aggregate blocks only their sign-in, through {@link SignInStatus}.
 */
public final class Customer {

    private final CustomerId id;
    private final SignInId signInId;
    private final TermsAcceptance termsAcceptance;
    private CustomerName name;
    private HomeAddress homeAddress;
    private ArrivalNote arrivalNote;
    private VerifiedIdentity verifiedIdentity;
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
     * and an arrival note, and no phone number (CUS-1), and who has agreed to the Terms of Service (CUS-2).
     * {@code signInStatus} is what Homa already knows about the sign-in: one that already holds an account (CUS-7),
     * or that stays blocked after a suspended customer's deletion (DAT-7), is refused. The new customer is active and
     * unverified (CUS-3), with a new id, so they start with no ratings and no reliability counts.
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

    /**
     * Rebuilds a customer from what was stored: the sign-in, the profile, the terms acceptance and, once they exist,
     * the verified identity and the latest suspension, each {@code null} when there is none. Every detail CUS-1 asks
     * for must be present. The sign-in (CUS-7) and identity (CUS-10) lookups aren't repeated: they held when the
     * customer registered and verified.
     */
    public static Customer restore(CustomerId id, SignInId signInId, CustomerName name, HomeAddress homeAddress,
            ArrivalNote arrivalNote, TermsAcceptance termsAcceptance, VerifiedIdentity verifiedIdentity,
            Suspension suspension) {
        Customer customer = new Customer(Objects.requireNonNull(id, "id"), Objects.requireNonNull(signInId, "signInId"),
                Required.detail(name, "a name"), Required.detail(homeAddress, "a home address"),
                Required.detail(arrivalNote, "an arrival note"),
                Objects.requireNonNull(termsAcceptance, "termsAcceptance"));
        customer.verifiedIdentity = verifiedIdentity;
        customer.suspension = suspension;
        return customer;
    }

    /** CUS-1: the customer goes by a new name. */
    public void changeName(CustomerName newName) {
        this.name = Required.detail(newName, "a name");
    }

    /**
     * CUS-1: the customer moves home; the new address has a city, which Reservation copies (RES-1). Reservations
     * already Requested keep the address they were made with.
     *
     * @return true when the new address differs from the one the customer had, so the caller announces the move;
     *         false when they entered the same address again
     */
    public boolean changeHomeAddress(HomeAddress newHomeAddress) {
        HomeAddress previous = homeAddress;
        this.homeAddress = Required.detail(newHomeAddress, "a home address");
        return !homeAddress.equals(previous);
    }

    /** CUS-1: the customer tells the team a new way to reach the home. */
    public void changeArrivalNote(ArrivalNote newArrivalNote) {
        this.arrivalNote = Required.detail(newArrivalNote, "an arrival note");
    }

    /**
     * CUS-3, CUS-10: records what Verification found when the customer verified their identity. The identity is
     * recorded whether or not it allows reserving, so a customer who isn't local or didn't pass the age check is told
     * why they can't book, instead of being asked to verify again; their personId stays held by this account.
     * {@code identityStatus} is what Homa already knows about the personId: one that another customer account holds
     * is refused (CUS-10). An account already verified as one person can't be verified as another, or that person's
     * identity would come free for a second account; the same person can verify again, for example once they come
     * of age.
     *
     * @return true when this verification is the one that first lets the customer's identity reserve, so the
     *         caller announces that the customer is verified; false when it doesn't allow reserving, or an earlier
     *         one already did
     */
    public boolean recordIdentityVerified(VerifiedIdentity identity, IdentityStatus identityStatus) {
        identityStatus.admitFor(id);
        if (isVerifiedAsSomeoneOtherThan(identity.personId())) {
            throw new AccountHoldsAnotherIdentityException(id.value());
        }
        boolean couldReserveBefore = identityAllowsReserving();
        this.verifiedIdentity = identity;
        return !couldReserveBefore && identity.allowsReserving();
    }

    private boolean isVerifiedAsSomeoneOtherThan(PersonId personId) {
        return verifiedIdentity != null && !verifiedIdentity.personId().equals(personId);
    }

    private boolean identityAllowsReserving() {
        return verifiedIdentity != null && verifiedIdentity.allowsReserving();
    }

    /**
     * CUS-4: Support's decision suspends the customer from its {@link Suspension#suspendedAt()}. A customer whose
     * suspension is in force then can't be suspended again; one whose suspension has ended can.
     */
    public void suspend(Suspension decided) {
        if (isSuspendedAt(decided.suspendedAt())) {
            throw new CustomerAlreadySuspendedException(id.value());
        }
        this.suspension = decided;
    }

    /**
     * CUS-6: Support lifts a suspension until lifted at {@code now}. A 7- or 30-day suspension ends only on its own.
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

    /**
     * What stops the customer from reserving at {@code now}: a suspension (CUS-5), no verified identity yet (CUS-3),
     * or a verified identity that isn't local or didn't pass the age check (CUS-10). Empty when they can reserve.
     */
    public Optional<ReservationBlocker> reservationBlockerAt(Instant now) {
        if (isSuspendedAt(now)) {
            return Optional.of(ReservationBlocker.SUSPENDED);
        }
        if (verifiedIdentity == null) {
            return Optional.of(ReservationBlocker.IDENTITY_NOT_VERIFIED);
        }
        return verifiedIdentity.reservationBlocker();
    }

    /**
     * CUS-3, CUS-5, CUS-10: true when the customer is verified, local, aged 18 or above, and not suspended at
     * {@code now}.
     */
    public boolean canReserveAt(Instant now) {
        return reservationBlockerAt(now).isEmpty();
    }

    /**
     * The customer's standing at {@code now}. An unverified customer is active: verification is needed only to
     * reserve, not to browse or start enquiries (CUS-3).
     */
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

    /** What Verification found about the customer's identity (CUS-3, CUS-10). Empty until they verify. */
    public Optional<VerifiedIdentity> verifiedIdentity() {
        return Optional.ofNullable(verifiedIdentity);
    }

    /** The customer's latest suspension, whether still in force or ended. Empty if never suspended. */
    public Optional<Suspension> suspension() {
        return Optional.ofNullable(suspension);
    }
}
