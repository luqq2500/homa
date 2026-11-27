package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.DANIEL_SUSPENDED_AT;
import static com.homa.customer.domain.CustomerFixtures.SAFETY_AND_MISCONDUCT;
import static com.homa.customer.domain.CustomerFixtures.SEVEN_DAYS_END;
import static com.homa.customer.domain.CustomerFixtures.aisyahJustRegistered;
import static com.homa.customer.domain.CustomerFixtures.aisyahsArrivalNote;
import static com.homa.customer.domain.CustomerFixtures.aisyahsGoogleAccount;
import static com.homa.customer.domain.CustomerFixtures.aisyahsHome;
import static com.homa.customer.domain.CustomerFixtures.aisyahsMyKad;
import static com.homa.customer.domain.CustomerFixtures.aisyahsName;
import static com.homa.customer.domain.CustomerFixtures.aisyahsTermsAcceptance;
import static com.homa.customer.domain.CustomerFixtures.danielSuspended;
import static com.homa.customer.domain.CustomerFixtures.husbandsMyKad;
import static com.homa.customer.domain.CustomerFixtures.supportSuspendsDaniel;
import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.exception.AccountHoldsAnotherIdentityException;
import com.homa.customer.domain.exception.CustomerAlreadySuspendedException;
import com.homa.customer.domain.exception.InconsistentSuspensionEndException;
import com.homa.customer.domain.exception.MissingCustomerDetailException;
import com.homa.customer.domain.exception.SuspensionEndsOnItsOwnException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredCustomerTest {

    /** Support reviews Daniel's appeal and lifts his suspension until lifted. */
    private static final Instant APPEAL_REVIEWED = at(2027, 3, 15, 11, 0);

    private static Customer restored(Customer stored) {
        return Customer.restore(stored.id(), stored.signInId(), stored.name(), stored.homeAddress(),
                stored.arrivalNote(), stored.termsAcceptance(), stored.verifiedIdentity().orElse(null),
                stored.suspension().map(StoredCustomerTest::restored).orElse(null));
    }

    private static Suspension restored(Suspension stored) {
        return Suspension.restore(stored.length(), stored.endsAt().orElse(null), stored.reason(),
                stored.suspendedAt(), stored.liftedAt().orElse(null));
    }

    private static Customer aisyahStoredWith(CustomerName name, HomeAddress home, ArrivalNote note) {
        return Customer.restore(CustomerId.generate(), aisyahsGoogleAccount(), name, home, note,
                aisyahsTermsAcceptance(), null, null);
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Persistence: Aisyah, registered and not yet verified, comes back with her profile and still verifies at her first reservation.")
    void aStoredUnverifiedCustomerComesBackAsSheWasSaved() {
        Customer aisyah = aisyahJustRegistered();

        Customer stored = restored(aisyah);

        assertThat(stored).usingRecursiveComparison().isEqualTo(aisyah);
        assertThat(stored.reservationBlockerAt(DANIEL_SUSPENDED_AT)).contains(ReservationBlocker.IDENTITY_NOT_VERIFIED);
        assertThat(stored.recordIdentityVerified(aisyahsMyKad(), IdentityStatus.free())).isTrue();
        assertThat(stored.canReserveAt(DANIEL_SUSPENDED_AT)).isTrue();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Persistence: Daniel, verified and with his suspension until lifted lifted after his appeal, comes back as he was saved and can be suspended again.")
    void aStoredCustomerKeepsHisVerifiedIdentityAndLiftedSuspension() {
        Customer daniel = danielSuspended(SuspensionLength.UNTIL_LIFTED);
        daniel.liftSuspension(APPEAL_REVIEWED);

        Customer stored = restored(daniel);

        assertThat(stored).usingRecursiveComparison().isEqualTo(daniel);
        assertThat(stored.suspension().orElseThrow().liftedAt()).contains(APPEAL_REVIEWED);
        assertThat(stored.isSuspendedAt(APPEAL_REVIEWED.minusNanos(1))).isTrue();
        assertThat(stored.statusAt(APPEAL_REVIEWED)).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(stored.canReserveAt(APPEAL_REVIEWED)).isTrue();
        assertThatThrownBy(() -> stored.recordIdentityVerified(husbandsMyKad(), IdentityStatus.free()))
                .isInstanceOf(AccountHoldsAnotherIdentityException.class);
        stored.suspend(Suspension.decided(SuspensionLength.SEVEN_DAYS, at(2027, 3, 22, 11, 0),
                "Repeated confirmed no-shows", at(2027, 3, 15, 11, 0)));
        assertThat(stored.isSuspendedAt(at(2027, 3, 16, 0, 0))).isTrue();
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("Persistence: a stored 7-day suspension still in force keeps Daniel from being suspended again.")
    void aStoredSuspensionInForceStaysInForce() {
        Customer daniel = danielSuspended(SuspensionLength.SEVEN_DAYS);

        Customer stored = restored(daniel);

        assertThat(stored).usingRecursiveComparison().isEqualTo(daniel);
        assertThat(stored.reservationBlockerAt(DANIEL_SUSPENDED_AT)).contains(ReservationBlocker.SUSPENDED);
        assertThatThrownBy(() -> stored.suspend(supportSuspendsDaniel(SuspensionLength.THIRTY_DAYS)))
                .isInstanceOf(CustomerAlreadySuspendedException.class);
        assertThat(stored.isSuspendedAt(SEVEN_DAYS_END)).isFalse();
    }

    @Test
    @Tag("CUS-1")
    @DisplayName("Persistence: a stored customer without a name, a home address or an arrival note is refused.")
    void aStoredCustomerMissingADetailIsRefused() {
        assertThatThrownBy(() -> aisyahStoredWith(null, aisyahsHome(), aisyahsArrivalNote()))
                .isInstanceOf(MissingCustomerDetailException.class)
                .hasMessage("CUS-1: a customer must have a name");
        assertThatThrownBy(() -> aisyahStoredWith(aisyahsName(), null, aisyahsArrivalNote()))
                .isInstanceOf(MissingCustomerDetailException.class)
                .hasMessage("CUS-1: a customer must have a home address");
        assertThatThrownBy(() -> aisyahStoredWith(aisyahsName(), aisyahsHome(), null))
                .isInstanceOf(MissingCustomerDetailException.class)
                .hasMessage("CUS-1: a customer must have an arrival note");
    }

    @Test
    @Tag("CUS-7")
    @DisplayName("Persistence: a stored customer without an id, a sign-in or the Terms of Service accepted is refused.")
    void aStoredCustomerMissingItsIdSignInOrTermsIsRefused() {
        assertThatThrownBy(() -> Customer.restore(null, aisyahsGoogleAccount(), aisyahsName(), aisyahsHome(),
                aisyahsArrivalNote(), aisyahsTermsAcceptance(), null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Customer.restore(CustomerId.generate(), null, aisyahsName(), aisyahsHome(),
                aisyahsArrivalNote(), aisyahsTermsAcceptance(), null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Customer.restore(CustomerId.generate(), aisyahsGoogleAccount(), aisyahsName(),
                aisyahsHome(), aisyahsArrivalNote(), null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Persistence: a stored suspension must fit together: a timed one ends after it starts and is never lifted, and every suspension has a start.")
    void aStoredSuspensionThatDoesNotFitTogetherIsRefused() {
        assertThatThrownBy(() -> Suspension.restore(SuspensionLength.SEVEN_DAYS, SEVEN_DAYS_END,
                SAFETY_AND_MISCONDUCT, DANIEL_SUSPENDED_AT, APPEAL_REVIEWED))
                .isInstanceOf(SuspensionEndsOnItsOwnException.class);
        assertThatThrownBy(() -> Suspension.restore(SuspensionLength.UNTIL_LIFTED, SEVEN_DAYS_END,
                SAFETY_AND_MISCONDUCT, DANIEL_SUSPENDED_AT, null))
                .isInstanceOf(InconsistentSuspensionEndException.class);
        assertThatThrownBy(() -> Suspension.restore(SuspensionLength.UNTIL_LIFTED, null, SAFETY_AND_MISCONDUCT,
                null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
