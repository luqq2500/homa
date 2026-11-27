package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.AISYAH_PERSON_ID;
import static com.homa.customer.domain.CustomerFixtures.DANIEL_PERSON_ID;
import static com.homa.customer.domain.CustomerFixtures.GOOGLE;
import static com.homa.customer.domain.CustomerFixtures.aisyah;
import static com.homa.customer.domain.CustomerFixtures.aisyahJustRegistered;
import static com.homa.customer.domain.CustomerFixtures.aisyahsHusband;
import static com.homa.customer.domain.CustomerFixtures.aisyahsMyKad;
import static com.homa.customer.domain.CustomerFixtures.aisyahsSon;
import static com.homa.customer.domain.CustomerFixtures.daniel;
import static com.homa.customer.domain.CustomerFixtures.danielRegistering;
import static com.homa.customer.domain.CustomerFixtures.danielsMyKad;
import static com.homa.customer.domain.CustomerFixtures.husbandsMyKad;
import static com.homa.customer.domain.CustomerFixtures.sonsMyKad;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.exception.AccountHoldsAnotherIdentityException;
import com.homa.customer.domain.exception.IdentityAlreadyHoldsAccountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerIdentityVerificationTest {

    @Test
    @Tag("CUS-3")
    @DisplayName("Aisyah's husband registers and asks Kemas Home about a ladder. When he taps Reserve, he's asked to "
            + "verify first.")
    void anUnverifiedCustomerCanEnquireButIsAskedToVerifyBeforeReserving() {
        Customer husband = aisyahsHusband();

        assertThat(husband.verifiedIdentity()).isEmpty();
        assertThat(husband.statusAt(REQUESTED_AT)).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(husband.canReserveAt(REQUESTED_AT)).isFalse();
        assertThat(husband.reservationBlockerAt(REQUESTED_AT)).contains(ReservationBlocker.IDENTITY_NOT_VERIFIED);

        boolean nowVerified = husband.recordIdentityVerified(husbandsMyKad(), IdentityStatus.free());

        assertThat(nowVerified).isTrue();
        assertThat(husband.verifiedIdentity()).contains(husbandsMyKad());
        assertThat(husband.canReserveAt(REQUESTED_AT)).isTrue();
        assertThat(husband.reservationBlockerAt(REQUESTED_AT)).isEmpty();
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("Aisyah is identity verified, so she can reserve Kemas Home; hearing of her verification again "
            + "changes nothing.")
    void aVerifiedCustomerCanReserveAndIsAnnouncedVerifiedOnce() {
        Customer aisyah = aisyahJustRegistered();

        assertThat(aisyah.recordIdentityVerified(aisyahsMyKad(), IdentityStatus.free())).isTrue();
        assertThat(aisyah.recordIdentityVerified(aisyahsMyKad(), IdentityStatus.heldBy(aisyah.id()))).isFalse();

        assertThat(aisyah.verifiedIdentity().orElseThrow().personId().value()).isEqualTo(AISYAH_PERSON_ID);
        assertThat(aisyah.canReserveAt(REQUESTED_AT)).isTrue();
        assertThat(aisyah().canReserveAt(REQUESTED_AT)).isTrue();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Aisyah verifies with her MyKad. Her 16-year-old son can't book.")
    void aCustomerUnderTheMinimumAgeCannotReserve() {
        Customer son = aisyahsSon();

        boolean nowVerified = son.recordIdentityVerified(sonsMyKad(false), IdentityStatus.free());

        assertThat(nowVerified).isFalse();
        assertThat(son.verifiedIdentity()).contains(sonsMyKad(false));
        assertThat(sonsMyKad(false).allowsReserving()).isFalse();
        assertThat(son.canReserveAt(REQUESTED_AT)).isFalse();
        assertThat(son.reservationBlockerAt(REQUESTED_AT)).contains(ReservationBlocker.UNDER_MINIMUM_AGE);
        assertThat(son.statusAt(REQUESTED_AT)).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("When Aisyah's son comes of age and verifies again with the same MyKad, he can book.")
    void aCustomerWhoComesOfAgeCanReserveAfterVerifyingAgain() {
        Customer son = aisyahsSon();
        son.recordIdentityVerified(sonsMyKad(false), IdentityStatus.free());

        boolean nowVerified = son.recordIdentityVerified(sonsMyKad(true), IdentityStatus.heldBy(son.id()));

        assertThat(nowVerified).isTrue();
        assertThat(son.canReserveAt(REQUESTED_AT)).isTrue();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("A foreign worker verified with an i-Kad can't book: in Malaysia only locals can be customers.")
    void aCustomerWhoIsNotLocalCannotReserve() {
        Customer foreign = aisyahsHusband();
        VerifiedIdentity iKad = new VerifiedIdentity(new PersonId("person-foreign-worker"), false, true);
        VerifiedIdentity foreignAndUnderAge = new VerifiedIdentity(new PersonId("person-foreign-worker"), false, false);

        assertThat(foreign.recordIdentityVerified(iKad, IdentityStatus.free())).isFalse();

        assertThat(iKad.allowsReserving()).isFalse();
        assertThat(foreign.canReserveAt(REQUESTED_AT)).isFalse();
        assertThat(foreign.reservationBlockerAt(REQUESTED_AT)).contains(ReservationBlocker.NOT_LOCAL);
        assertThat(foreignAndUnderAge.reservationBlocker()).contains(ReservationBlocker.NOT_LOCAL);
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Daniel can't open a new account with another Google account, because his MyKad is already used.")
    void aVerifiedIdentityHoldsOnlyOneCustomerAccount() {
        Customer first = daniel();
        Customer second = danielRegistering(new SignInId(GOOGLE, "100000000000000000042"), SignInStatus.free());
        IdentityStatus heldByFirst = IdentityStatus.heldBy(first.id());

        assertThat(heldByFirst.holder()).contains(first.id());
        assertThatThrownBy(() -> second.recordIdentityVerified(danielsMyKad(), heldByFirst))
                .isInstanceOf(IdentityAlreadyHoldsAccountException.class)
                .hasMessage("CUS-10: this verified identity already holds a Homa customer account");
        assertThat(second.verifiedIdentity()).isEmpty();
        assertThat(second.reservationBlockerAt(REQUESTED_AT)).contains(ReservationBlocker.IDENTITY_NOT_VERIFIED);
        assertThat(IdentityStatus.free().holder()).isEmpty();
        assertThat(first.canReserveAt(REQUESTED_AT)).isTrue();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Daniel can't verify a friend's MyKad on his account to free his own MyKad for a second account.")
    void anAccountVerifiedAsOnePersonCannotBeVerifiedAsAnother() {
        Customer daniel = daniel();
        VerifiedIdentity friendsMyKad = new VerifiedIdentity(new PersonId("person-daniels-friend"), true, true);

        assertThatThrownBy(() -> daniel.recordIdentityVerified(friendsMyKad, IdentityStatus.free()))
                .isInstanceOf(AccountHoldsAnotherIdentityException.class)
                .hasMessage("CUS-10: customer " + daniel.id().value() + " is already verified as another person");
        assertThat(daniel.verifiedIdentity().orElseThrow().personId().value()).isEqualTo(DANIEL_PERSON_ID);
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Verification always says whose identity it verified.")
    void aVerifiedIdentityNamesThePerson() {
        assertThatThrownBy(() -> new VerifiedIdentity(null, true, true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PersonId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PersonId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new PersonId(AISYAH_PERSON_ID).value()).isEqualTo(AISYAH_PERSON_ID);
        assertThat(aisyahsMyKad().local()).isTrue();
        assertThat(aisyahsMyKad().ageCheckPassed()).isTrue();
    }
}
