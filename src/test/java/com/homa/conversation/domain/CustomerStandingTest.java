package com.homa.conversation.domain;

import static com.homa.conversation.domain.ConversationFixtures.AISYAH;
import static com.homa.conversation.domain.ConversationFixtures.DANIEL;
import static com.homa.conversation.domain.ConversationFixtures.REQUESTED_AT;
import static com.homa.conversation.domain.ConversationFixtures.aisyah;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.conversation.domain.exception.SuspendedCustomerCannotStartEnquiryException;
import com.homa.fixtures.HomaFixtures;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerStandingTest {

    private static final Instant SUSPENDED_ON = HomaFixtures.at(2026, 12, 1, 9, 0);
    private static final Instant THIRTY_DAYS_LATER = HomaFixtures.at(2026, 12, 31, 9, 0);
    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    @Test
    @Tag("CHT-1")
    @DisplayName("Aisyah's husband registers and asks Kemas Home about a ladder: a new customer is in good standing "
            + "though not identity verified, because enquiries don't need verification (CUS-3).")
    void aNewlyRegisteredCustomerIsInGoodStandingBeforeVerifying() {
        CustomerStanding husband = CustomerStanding.registered(AISYAH);

        assertThat(husband.customerId()).isEqualTo(AISYAH);
        assertThat(husband.identityVerified()).isFalse();
        assertThat(husband.suspension()).isEmpty();
        assertThat(husband.isSuspendedAt(REQUESTED_AT)).isFalse();
        assertThatCode(() -> husband.requireCanStartEnquiryAt(REQUESTED_AT)).doesNotThrowAnyException();
    }

    @Test
    @Tag("CHT-1")
    @DisplayName("When Aisyah's husband verifies his identity he stays in good standing for enquiries.")
    void aVerifiedCustomerIsInGoodStanding() {
        CustomerStanding verified = aisyah().verified();

        assertThat(verified.identityVerified()).isTrue();
        assertThat(verified.customerId()).isEqualTo(AISYAH);
        assertThatCode(() -> verified.requireCanStartEnquiryAt(REQUESTED_AT)).doesNotThrowAnyException();
    }

    @Test
    @Tag("CHT-1")
    @Tag("CUS-5")
    @DisplayName("Support suspends Daniel for 30 days: a suspended customer can't start enquiries.")
    void aSuspendedCustomerIsNotInGoodStanding() {
        CustomerStanding daniel = CustomerStanding.registered(DANIEL).suspended(SUSPENDED_ON, THIRTY_DAYS_LATER);

        assertThat(daniel.isSuspendedAt(SUSPENDED_ON.plus(Duration.ofDays(1)))).isTrue();
        assertThatThrownBy(() -> daniel.requireCanStartEnquiryAt(SUSPENDED_ON.plus(Duration.ofDays(1))))
                .isInstanceOf(SuspendedCustomerCannotStartEnquiryException.class)
                .hasMessageContaining(DANIEL.value());
        assertThat(daniel.suspension()).contains(new Suspension(SUSPENDED_ON, THIRTY_DAYS_LATER));
        assertThat(daniel.customerId()).isEqualTo(DANIEL);
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Daniel can book again after 30 days: a timed suspension ends by itself, with nothing stored "
            + "changing.")
    void aTimedSuspensionEndsByItself() {
        CustomerStanding daniel = CustomerStanding.registered(DANIEL).suspended(SUSPENDED_ON, THIRTY_DAYS_LATER);

        assertThat(daniel.isSuspendedAt(THIRTY_DAYS_LATER.minus(ONE_MINUTE))).isTrue();
        assertThat(daniel.isSuspendedAt(THIRTY_DAYS_LATER)).isFalse();
        assertThatCode(() -> daniel.requireCanStartEnquiryAt(THIRTY_DAYS_LATER)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A suspension does not apply before it starts, and applies from the moment it does.")
    void aSuspensionAppliesFromItsStart() {
        CustomerStanding daniel = CustomerStanding.registered(DANIEL).suspended(SUSPENDED_ON, THIRTY_DAYS_LATER);

        assertThat(daniel.isSuspendedAt(SUSPENDED_ON.minus(ONE_MINUTE))).isFalse();
        assertThat(daniel.isSuspendedAt(SUSPENDED_ON)).isTrue();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("A customer suspended until lifted waits for Support: the suspension never ends by itself.")
    void aSuspensionUntilLiftedNeverEndsByItself() {
        CustomerStanding daniel = CustomerStanding.registered(DANIEL).suspended(SUSPENDED_ON, null);

        assertThat(daniel.isSuspendedAt(HomaFixtures.at(2030, 1, 1, 9, 0))).isTrue();
        assertThat(daniel.suspension()).contains(new Suspension(SUSPENDED_ON, null));
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Support lifts Daniel's suspension after reviewing his appeal: he is in good standing again.")
    void aReactivatedCustomerIsInGoodStandingAgain() {
        CustomerStanding daniel = CustomerStanding.registered(DANIEL).verified().suspended(SUSPENDED_ON, null);

        CustomerStanding reactivated = daniel.reactivated();

        assertThat(reactivated.isSuspendedAt(SUSPENDED_ON.plus(Duration.ofDays(2)))).isFalse();
        assertThat(reactivated.suspension()).isEmpty();
        assertThat(reactivated.identityVerified()).isTrue();
        assertThat(reactivated.customerId()).isEqualTo(DANIEL);
    }

    @Test
    @DisplayName("Verifying a suspended customer leaves the suspension, and suspending a verified one the verification.")
    void verifyingAndSuspendingDoNotUndoEachOther() {
        CustomerStanding suspended = CustomerStanding.registered(DANIEL).suspended(SUSPENDED_ON, null).verified();
        CustomerStanding verified = CustomerStanding.registered(DANIEL).verified().suspended(SUSPENDED_ON, null);

        assertThat(suspended.identityVerified()).isTrue();
        assertThat(suspended.isSuspendedAt(SUSPENDED_ON)).isTrue();
        assertThat(verified.identityVerified()).isTrue();
        assertThat(verified.isSuspendedAt(SUSPENDED_ON)).isTrue();
    }

    @Test
    @DisplayName("A suspension that ends must end after it starts, and needs a start.")
    void aSuspensionMustEndAfterItStarts() {
        assertThatThrownBy(() -> new Suspension(SUSPENDED_ON, SUSPENDED_ON))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Suspension(SUSPENDED_ON, SUSPENDED_ON.minus(ONE_MINUTE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new Suspension(SUSPENDED_ON, SUSPENDED_ON.plus(ONE_MINUTE)).endsAt())
                .isEqualTo(SUSPENDED_ON.plus(ONE_MINUTE));
        assertThatThrownBy(() -> new Suspension(null, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("A stored standing is rebuilt as it was, with or without a suspension.")
    void aStoredStandingIsRebuiltAsItWas() {
        Suspension suspension = new Suspension(SUSPENDED_ON, THIRTY_DAYS_LATER);

        CustomerStanding suspended = CustomerStanding.restore(DANIEL, true, suspension);
        CustomerStanding clear = CustomerStanding.restore(AISYAH, false, null);

        assertThat(suspended.customerId()).isEqualTo(DANIEL);
        assertThat(suspended.identityVerified()).isTrue();
        assertThat(suspended.suspension()).contains(suspension);
        assertThat(clear.identityVerified()).isFalse();
        assertThat(clear.suspension()).isEmpty();
        assertThatThrownBy(() -> CustomerStanding.restore(null, false, null))
                .isInstanceOf(NullPointerException.class);
    }
}
