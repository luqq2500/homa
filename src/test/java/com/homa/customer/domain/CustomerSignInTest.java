package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.AISYAH_GOOGLE_SUBJECT;
import static com.homa.customer.domain.CustomerFixtures.APPLE;
import static com.homa.customer.domain.CustomerFixtures.DANIEL_GOOGLE_SUBJECT;
import static com.homa.customer.domain.CustomerFixtures.DANIEL_SUSPENDED_AT;
import static com.homa.customer.domain.CustomerFixtures.GOOGLE;
import static com.homa.customer.domain.CustomerFixtures.aisyah;
import static com.homa.customer.domain.CustomerFixtures.aisyahsArrivalNote;
import static com.homa.customer.domain.CustomerFixtures.aisyahsHome;
import static com.homa.customer.domain.CustomerFixtures.aisyahsName;
import static com.homa.customer.domain.CustomerFixtures.aisyahsTermsAcceptance;
import static com.homa.customer.domain.CustomerFixtures.danielRegistering;
import static com.homa.customer.domain.CustomerFixtures.danielSuspended;
import static com.homa.customer.domain.CustomerFixtures.danielsGoogleAccount;
import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.exception.MissingSignInException;
import com.homa.customer.domain.exception.SignInAlreadyHoldsAccountException;
import com.homa.customer.domain.exception.SignInBlockedException;
import java.time.Instant;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerSignInTest {

    @Test
    @Tag("CUS-7")
    @DisplayName("Aisyah signs in with her Google account.")
    void customerRegistersWithTheIdentityProviderAccountSheSignedInWith() {
        assertThat(SignInStatus.free().state()).isEqualTo(SignInStatus.State.FREE);
        assertThat(SignInStatus.free().blockedUntil()).isEmpty();

        Customer aisyah = aisyah();

        assertThat(aisyah.signInId().issuer()).isEqualTo(GOOGLE);
        assertThat(aisyah.signInId().subject()).isEqualTo(AISYAH_GOOGLE_SUBJECT);
    }

    @Test
    @Tag("CUS-7")
    @DisplayName("While suspended, Daniel can't open a second Homa account with the same Google account.")
    void aSuspendedCustomerCannotOpenASecondAccountWithTheSameIdentityProviderAccount() {
        Customer daniel = danielSuspended(SuspensionPeriod.THIRTY_DAYS);
        Instant nextDay = at(2026, 12, 3, 15, 0);
        assertThat(daniel.isSuspendedAt(nextDay)).isTrue();

        SignInStatus heldByDaniel = SignInStatus.taken();

        assertThat(heldByDaniel.state()).isEqualTo(SignInStatus.State.TAKEN);
        assertThat(heldByDaniel.blockedUntil()).isEmpty();
        assertThatThrownBy(() -> danielRegistering(danielsGoogleAccount(), heldByDaniel))
                .isInstanceOf(SignInAlreadyHoldsAccountException.class)
                .hasMessage("CUS-7: this " + GOOGLE + " account already holds a Homa customer account");
    }

    @Test
    @Tag("CUS-7")
    @DisplayName("A customer who signs up with a different Google account starts with no ratings or counts.")
    void aDifferentIdentityProviderAccountOpensANewAccountWithItsOwnId() {
        Customer first = danielRegistering(danielsGoogleAccount(), SignInStatus.free());
        SignInId otherGoogleAccount = new SignInId(GOOGLE, "100000000000000000042");
        SignInId sameSubjectAtApple = new SignInId(APPLE, DANIEL_GOOGLE_SUBJECT);

        Customer second = danielRegistering(otherGoogleAccount, SignInStatus.free());
        Customer third = danielRegistering(sameSubjectAtApple, SignInStatus.free());

        assertThat(otherGoogleAccount).isNotEqualTo(danielsGoogleAccount());
        assertThat(sameSubjectAtApple).isNotEqualTo(danielsGoogleAccount());
        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(third.id()).isNotEqualTo(first.id()).isNotEqualTo(second.id());
        assertThat(second.signInId()).isEqualTo(otherGoogleAccount);
        assertThat(second.suspension()).isEmpty();
    }

    @Test
    @Tag("CUS-7")
    @DisplayName("Aisyah can't register without signing in through an identity provider that gives its issuer and "
            + "her account's subject.")
    void customerCannotRegisterWithoutAnIdentityProviderSignIn() {
        assertThatThrownBy(() -> Customer.register(null, SignInStatus.free(), aisyahsName(), aisyahsHome(),
                aisyahsArrivalNote(), aisyahsTermsAcceptance()))
                .isInstanceOf(MissingSignInException.class)
                .hasMessage("CUS-7: a customer must have a sign-in from an identity provider");
        assertMissingPart(() -> new SignInId(null, AISYAH_GOOGLE_SUBJECT), "an issuer");
        assertMissingPart(() -> new SignInId(" ", AISYAH_GOOGLE_SUBJECT), "an issuer");
        assertMissingPart(() -> new SignInId(GOOGLE, null), "a subject");
        assertMissingPart(() -> new SignInId(GOOGLE, "\t"), "a subject");
        assertThatThrownBy(() -> Customer.register(new SignInId(GOOGLE, AISYAH_GOOGLE_SUBJECT), null, aisyahsName(),
                aisyahsHome(), aisyahsArrivalNote(), aisyahsTermsAcceptance()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("CUS-9")
    @DisplayName("Daniel, suspended until lifted, can't use deletion to sign up again with the same Google account.")
    void anErasedCustomersSignInStaysBlockedUntilSupportLiftsTheirSuspension() {
        Customer daniel = danielSuspended(SuspensionPeriod.UNTIL_LIFTED);
        Suspension untilLifted = daniel.suspension().orElseThrow();
        Instant aYearLater = at(2027, 12, 2, 15, 0);

        SignInStatus blocked = SignInStatus.blockedWhile(untilLifted, aYearLater);

        assertThat(blocked.state()).isEqualTo(SignInStatus.State.BLOCKED);
        assertThat(blocked.blockedUntil()).isEmpty();
        assertThatThrownBy(() -> danielRegistering(danielsGoogleAccount(), blocked))
                .isInstanceOfSatisfying(SignInBlockedException.class, e -> assertThat(e.blockedUntil()).isEmpty())
                .hasMessage("CUS-9: this " + GOOGLE + " account is blocked until Support lifts its suspension");

        daniel.liftSuspension(aYearLater);
        SignInStatus afterAppeal = SignInStatus.blockedWhile(daniel.suspension().orElseThrow(), aYearLater);

        assertThat(afterAppeal.state()).isEqualTo(SignInStatus.State.FREE);
        assertThat(danielRegistering(danielsGoogleAccount(), afterAppeal).signInId()).isEqualTo(danielsGoogleAccount());
    }

    @Test
    @Tag("CUS-9")
    @DisplayName("A customer suspended for a set period who deletes their data can sign up again with the same Google "
            + "account once the suspension would have ended.")
    void anErasedCustomersSignInIsFreeOnceTheirTimedSuspensionEnds() {
        Suspension thirtyDays = danielSuspended(SuspensionPeriod.THIRTY_DAYS).suspension().orElseThrow();
        Instant endsAt = at(2027, 1, 1, 15, 0);

        SignInStatus aMomentBefore = SignInStatus.blockedWhile(thirtyDays, endsAt.minusNanos(1));

        assertThat(aMomentBefore.state()).isEqualTo(SignInStatus.State.BLOCKED);
        assertThat(aMomentBefore.blockedUntil()).contains(endsAt);
        assertThatThrownBy(() -> danielRegistering(danielsGoogleAccount(), aMomentBefore))
                .isInstanceOfSatisfying(SignInBlockedException.class,
                        e -> assertThat(e.blockedUntil()).contains(endsAt))
                .hasMessage("CUS-9: this " + GOOGLE + " account is blocked until " + endsAt);
        assertThat(SignInStatus.blockedWhile(thirtyDays, endsAt)).isSameAs(SignInStatus.free());
        assertThat(SignInStatus.blockedWhile(thirtyDays, DANIEL_SUSPENDED_AT).state())
                .isEqualTo(SignInStatus.State.BLOCKED);
    }

    private static void assertMissingPart(ThrowingCallable signIn, String part) {
        assertThatThrownBy(signIn)
                .isInstanceOf(MissingSignInException.class)
                .hasMessage("CUS-7: a customer must have a sign-in with " + part + " from an identity provider");
    }
}
