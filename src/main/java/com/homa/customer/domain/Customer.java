package com.homa.customer.domain;

import com.homa.customer.domain.exception.CustomerAlreadySuspendedException;
import com.homa.customer.domain.exception.CustomerNotSuspendedException;
import com.homa.customer.domain.exception.MissingSuspensionReasonException;
import com.homa.customer.domain.exception.TermsOfServiceNotAcceptedException;
import java.util.Optional;

/**
 * A registered homeowner who makes reservations. A customer exists only through {@link #register}, so only a
 * registered customer can make reservations (CUS-3). Every detail CUS-1 asks for is always present, at registration
 * and after any profile change.
 */
public final class Customer {

    private final CustomerId id;
    private final VerifiedPhone verifiedPhone;
    private final TermsAcceptance termsAcceptance;
    private UserName userName;
    private HomeAddress homeAddress;
    private ArrivalNote arrivalNote;
    private CustomerStatus status;
    private String suspensionReason;

    private Customer(CustomerId id, VerifiedPhone verifiedPhone, UserName userName, HomeAddress homeAddress,
            ArrivalNote arrivalNote, TermsAcceptance termsAcceptance) {
        this.id = id;
        this.verifiedPhone = verifiedPhone;
        this.userName = userName;
        this.homeAddress = homeAddress;
        this.arrivalNote = arrivalNote;
        this.termsAcceptance = termsAcceptance;
        this.status = CustomerStatus.ACTIVE;
    }

    /**
     * Registers a customer with a verified phone number, a user name, a home address with city and an arrival note
     * (CUS-1), who has agreed to the Terms of Service (CUS-2). The new customer is active.
     */
    public static Customer register(VerifiedPhone verifiedPhone, UserName userName, HomeAddress homeAddress,
            ArrivalNote arrivalNote, TermsAcceptance termsAcceptance) {
        Required.detail(verifiedPhone, "a verified phone number");
        Required.detail(userName, "a user name");
        Required.detail(homeAddress, "a home address");
        Required.detail(arrivalNote, "an arrival note");
        if (termsAcceptance == null) {
            throw new TermsOfServiceNotAcceptedException();
        }
        return new Customer(CustomerId.generate(), verifiedPhone, userName, homeAddress, arrivalNote, termsAcceptance);
    }

    /** CUS-1: the customer goes by a new user name. */
    public void changeUserName(UserName newUserName) {
        this.userName = Required.detail(newUserName, "a user name");
    }

    /** CUS-1: the customer moves home; the new address has a city, which Reservation copies (RES-1). */
    public void changeHomeAddress(HomeAddress newHomeAddress) {
        this.homeAddress = Required.detail(newHomeAddress, "a home address");
    }

    /** CUS-1: the customer tells the team a new way to reach the home. */
    public void changeArrivalNote(ArrivalNote newArrivalNote) {
        this.arrivalNote = Required.detail(newArrivalNote, "an arrival note");
    }

    /** CUS-4: Support suspends an active customer, giving the reason. */
    public void suspend(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new MissingSuspensionReasonException();
        }
        if (status == CustomerStatus.SUSPENDED) {
            throw new CustomerAlreadySuspendedException(id.value());
        }
        this.status = CustomerStatus.SUSPENDED;
        this.suspensionReason = reason;
    }

    /** CUS-6: a suspended customer becomes active again. Support's confirmation is checked by the caller. */
    public void reactivate() {
        if (status != CustomerStatus.SUSPENDED) {
            throw new CustomerNotSuspendedException(id.value());
        }
        this.status = CustomerStatus.ACTIVE;
        this.suspensionReason = null;
    }

    /** CUS-3: a registered customer can make reservations unless Support has suspended them (CUS-5). */
    public boolean canMakeReservations() {
        return status == CustomerStatus.ACTIVE;
    }

    public CustomerId id() {
        return id;
    }

    public VerifiedPhone verifiedPhone() {
        return verifiedPhone;
    }

    public UserName userName() {
        return userName;
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

    public CustomerStatus status() {
        return status;
    }

    /** Why Support suspended the customer, while they are suspended. */
    public Optional<String> suspensionReason() {
        return Optional.ofNullable(suspensionReason);
    }
}
