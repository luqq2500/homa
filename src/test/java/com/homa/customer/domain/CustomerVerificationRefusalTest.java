package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.DANIEL_SUSPENDED_AT;
import static com.homa.customer.domain.CustomerFixtures.aisyahsHusband;
import static com.homa.customer.domain.CustomerFixtures.aisyahsSon;
import static com.homa.customer.domain.CustomerFixtures.danielSuspended;
import static com.homa.customer.domain.CustomerFixtures.sonsMyKad;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerVerificationRefusalTest {

    @Test
    @Tag("CUS-10")
    @DisplayName("Aisyah's 16-year-old son can't book: Verification refused him as under 18, and he's told so instead "
            + "of being asked to verify again.")
    void anUnderageCustomerIsToldWhyHeCantReserve() {
        Customer son = aisyahsSon();

        son.recordVerificationRefused(VerificationRefusal.UNDER_MINIMUM_AGE);

        assertThat(son.verificationRefusal()).contains(VerificationRefusal.UNDER_MINIMUM_AGE);
        assertThat(son.verifiedIdentity()).isEmpty();
        assertThat(son.reservationBlockerAt(REQUESTED_AT)).contains(ReservationBlocker.UNDER_MINIMUM_AGE);
        assertThat(son.canReserveAt(REQUESTED_AT)).isFalse();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("A foreign visitor can't be a customer in Malaysia: they're told their status doesn't allow it.")
    void aForeignVisitorIsToldWhyTheyCantReserve() {
        Customer visitor = aisyahsHusband();

        visitor.recordVerificationRefused(VerificationRefusal.NOT_LOCAL);

        assertThat(visitor.reservationBlockerAt(REQUESTED_AT)).contains(ReservationBlocker.NOT_LOCAL);
    }

    @Test
    @Tag("CUS-10")
    @Tag("IDN-3")
    @DisplayName("Daniel's second account can't reserve: his MyKad already holds his first account.")
    void aSecondAccountIsToldTheIdentityIsHeld() {
        Customer secondAccount = aisyahsHusband();

        secondAccount.recordVerificationRefused(VerificationRefusal.IDENTITY_HELD_BY_ANOTHER_ACCOUNT);

        assertThat(secondAccount.reservationBlockerAt(REQUESTED_AT))
                .contains(ReservationBlocker.IDENTITY_HELD_BY_ANOTHER_ACCOUNT);
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Once the son comes of age and verifies again, the old refusal no longer stops him.")
    void aPassingVerificationSupersedesTheRefusal() {
        Customer son = aisyahsSon();
        son.recordVerificationRefused(VerificationRefusal.UNDER_MINIMUM_AGE);

        son.recordIdentityVerified(sonsMyKad(true), IdentityStatus.free());

        assertThat(son.reservationBlockerAt(REQUESTED_AT)).isEmpty();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("A later refusal replaces the earlier one.")
    void aLaterRefusalReplacesTheEarlierOne() {
        Customer customer = aisyahsHusband();
        customer.recordVerificationRefused(VerificationRefusal.UNDER_MINIMUM_AGE);

        customer.recordVerificationRefused(VerificationRefusal.NOT_LOCAL);

        assertThat(customer.verificationRefusal()).contains(VerificationRefusal.NOT_LOCAL);
        assertThat(customer.reservationBlockerAt(REQUESTED_AT)).contains(ReservationBlocker.NOT_LOCAL);
    }

    @Test
    @Tag("CUS-5")
    @DisplayName("A suspended customer is told of the suspension first, whatever Verification refused.")
    void aSuspensionIsNamedBeforeTheRefusal() {
        Customer daniel = danielSuspended(SuspensionLength.SEVEN_DAYS);

        daniel.recordVerificationRefused(VerificationRefusal.IDENTITY_HELD_BY_ANOTHER_ACCOUNT);

        assertThat(daniel.reservationBlockerAt(DANIEL_SUSPENDED_AT)).contains(ReservationBlocker.SUSPENDED);
    }

    @Test
    @DisplayName("A customer never refused has no refusal, and a refusal must say why.")
    void aRefusalMustSayWhy() {
        Customer husband = aisyahsHusband();

        assertThat(husband.verificationRefusal()).isEmpty();
        assertThatThrownBy(() -> husband.recordVerificationRefused(null)).isInstanceOf(NullPointerException.class);
        assertThat(husband.verificationRefusal()).isEmpty();
    }
}
