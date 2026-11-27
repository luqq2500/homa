package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.DANIEL_SUSPENDED_AT;
import static com.homa.customer.domain.CustomerFixtures.SAFETY_AND_MISCONDUCT;
import static com.homa.customer.domain.CustomerFixtures.SEVEN_DAYS_END;
import static com.homa.customer.domain.CustomerFixtures.THIRTY_DAYS_END;
import static com.homa.customer.domain.CustomerFixtures.daniel;
import static com.homa.customer.domain.CustomerFixtures.danielSuspended;
import static com.homa.customer.domain.CustomerFixtures.supportSuspendsDaniel;
import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.exception.CustomerAlreadySuspendedException;
import com.homa.customer.domain.exception.CustomerNotSuspendedException;
import com.homa.customer.domain.exception.SuspensionEndsOnItsOwnException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerSuspensionTest {

    private static final Instant NEXT_DAY = at(2026, 12, 3, 15, 0);

    @Test
    @Tag("CUS-4")
    @DisplayName("Daniel threatens Kemas Home's helpers. Support confirms the report and suspends him for 30 days.")
    void supportSuspendsACustomerForThirtyDaysAfterAConfirmedReport() {
        Customer daniel = danielSuspended(SuspensionLength.THIRTY_DAYS);

        Suspension suspension = daniel.suspension().orElseThrow();
        assertThat(suspension.length()).isEqualTo(SuspensionLength.THIRTY_DAYS);
        assertThat(suspension.reason()).isEqualTo(SAFETY_AND_MISCONDUCT);
        assertThat(suspension.suspendedAt()).isEqualTo(DANIEL_SUSPENDED_AT);
        assertThat(suspension.endsAt()).contains(THIRTY_DAYS_END);
        assertThat(suspension.liftedAt()).isEmpty();
        assertThat(daniel.isSuspendedAt(DANIEL_SUSPENDED_AT)).isTrue();
        assertThat(daniel.statusAt(NEXT_DAY)).isEqualTo(CustomerStatus.SUSPENDED);
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("Support can instead suspend Daniel for 7 days, or until Support lifts it.")
    void supportSuspendsACustomerForSevenDaysOrUntilLifted() {
        Customer sevenDays = danielSuspended(SuspensionLength.SEVEN_DAYS);
        Customer untilLifted = danielSuspended(SuspensionLength.UNTIL_LIFTED);

        assertThat(sevenDays.suspension().orElseThrow().endsAt()).contains(SEVEN_DAYS_END);
        assertThat(sevenDays.isSuspendedAt(NEXT_DAY)).isTrue();
        assertThat(untilLifted.suspension().orElseThrow().endsAt()).isEmpty();
        assertThat(untilLifted.isSuspendedAt(NEXT_DAY)).isTrue();
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("Support can't suspend Daniel again while his suspension is in force.")
    void supportCannotSuspendACustomerWhoseSuspensionIsInForce() {
        Customer daniel = daniel();
        assertThat(daniel.suspension()).isEmpty();
        assertThat(daniel.statusAt(DANIEL_SUSPENDED_AT)).isEqualTo(CustomerStatus.ACTIVE);

        daniel.suspend(supportSuspendsDaniel(SuspensionLength.THIRTY_DAYS));
        Suspension again = Suspension.decided(SuspensionLength.UNTIL_LIFTED, null, "Repeated confirmed no-shows",
                THIRTY_DAYS_END.minusNanos(1));

        assertThatThrownBy(() -> daniel.suspend(again))
                .isInstanceOf(CustomerAlreadySuspendedException.class)
                .hasMessage("CUS-4: customer " + daniel.id().value() + " is already suspended");
        assertThat(daniel.suspension().orElseThrow().reason()).isEqualTo(SAFETY_AND_MISCONDUCT);
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("After Daniel's suspension has ended, Support can suspend him again for repeated confirmed no-shows.")
    void supportSuspendsACustomerAgainOnceTheEarlierSuspensionHasEnded() {
        Customer daniel = danielSuspended(SuspensionLength.SEVEN_DAYS);

        daniel.suspend(Suspension.decided(SuspensionLength.UNTIL_LIFTED, null, "Repeated confirmed no-shows",
                SEVEN_DAYS_END));

        Suspension again = daniel.suspension().orElseThrow();
        assertThat(again.length()).isEqualTo(SuspensionLength.UNTIL_LIFTED);
        assertThat(again.reason()).isEqualTo("Repeated confirmed no-shows");
        assertThat(again.suspendedAt()).isEqualTo(SEVEN_DAYS_END);
        assertThat(daniel.isSuspendedAt(SEVEN_DAYS_END)).isTrue();
    }

    @Test
    @Tag("CUS-5")
    @DisplayName("While suspended, Daniel can't reserve, though his identity is verified; he can still see why.")
    void aSuspendedCustomerCannotReserve() {
        Customer daniel = danielSuspended(SuspensionLength.THIRTY_DAYS);

        assertThat(daniel.verifiedIdentity()).isPresent();
        assertThat(daniel.canReserveAt(NEXT_DAY)).isFalse();
        assertThat(daniel.reservationBlockerAt(NEXT_DAY)).contains(ReservationBlocker.SUSPENDED);
        assertThat(daniel.canReserveAt(DANIEL_SUSPENDED_AT.minusNanos(1))).isTrue();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Daniel can book again after 30 days.")
    void aThirtyDaySuspensionEndsByItself() {
        Customer daniel = danielSuspended(SuspensionLength.THIRTY_DAYS);
        Suspension suspension = daniel.suspension().orElseThrow();

        assertThat(daniel.isSuspendedAt(THIRTY_DAYS_END.minusNanos(1))).isTrue();
        assertThat(daniel.canReserveAt(THIRTY_DAYS_END.minusNanos(1))).isFalse();

        assertThat(daniel.isSuspendedAt(THIRTY_DAYS_END)).isFalse();
        assertThat(daniel.canReserveAt(THIRTY_DAYS_END)).isTrue();
        assertThat(daniel.reservationBlockerAt(THIRTY_DAYS_END)).isEmpty();
        assertThat(daniel.statusAt(THIRTY_DAYS_END)).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(daniel.suspension()).containsSame(suspension);
        assertThat(suspension.liftedAt()).isEmpty();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("A customer suspended until lifted waits for Support to review an appeal.")
    void aSuspensionUntilLiftedEndsWhenSupportLiftsIt() {
        Customer daniel = danielSuspended(SuspensionLength.UNTIL_LIFTED);
        Instant appealReviewed = at(2027, 3, 15, 11, 0);

        assertThat(daniel.isSuspendedAt(at(2027, 3, 1, 0, 0))).isTrue();

        daniel.liftSuspension(appealReviewed);

        Suspension lifted = daniel.suspension().orElseThrow();
        assertThat(lifted.liftedAt()).contains(appealReviewed);
        assertThat(lifted.endsAt()).isEmpty();
        assertThat(lifted.length()).isEqualTo(SuspensionLength.UNTIL_LIFTED);
        assertThat(lifted.reason()).isEqualTo(SAFETY_AND_MISCONDUCT);
        assertThat(lifted.suspendedAt()).isEqualTo(DANIEL_SUSPENDED_AT);
        assertThat(daniel.isSuspendedAt(appealReviewed.minusNanos(1))).isTrue();
        assertThat(daniel.isSuspendedAt(appealReviewed)).isFalse();
        assertThat(daniel.canReserveAt(appealReviewed)).isTrue();
        assertThat(daniel.statusAt(appealReviewed)).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Support can't end Daniel's 30-day suspension early; it ends by itself.")
    void supportCannotLiftASuspensionThatEndsByItself() {
        Customer daniel = danielSuspended(SuspensionLength.THIRTY_DAYS);

        assertThatThrownBy(() -> daniel.liftSuspension(NEXT_DAY))
                .isInstanceOf(SuspensionEndsOnItsOwnException.class)
                .hasMessage("CUS-6: a THIRTY_DAYS suspension ends on its own at " + THIRTY_DAYS_END
                        + "; Support lifts only a suspension until lifted");
        assertThat(daniel.isSuspendedAt(NEXT_DAY)).isTrue();
        assertThat(daniel.suspension().orElseThrow().liftedAt()).isEmpty();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("When Daniel isn't suspended, there is no suspension for Support to lift.")
    void supportCannotLiftASuspensionThatIsNotInForce() {
        Customer neverSuspended = daniel();
        Customer timedOut = danielSuspended(SuspensionLength.SEVEN_DAYS);
        Customer alreadyLifted = danielSuspended(SuspensionLength.UNTIL_LIFTED);
        alreadyLifted.liftSuspension(NEXT_DAY);

        assertThatThrownBy(() -> neverSuspended.liftSuspension(NEXT_DAY))
                .isInstanceOf(CustomerNotSuspendedException.class)
                .hasMessage("CUS-6: customer " + neverSuspended.id().value()
                        + " is not suspended, so there is no suspension to lift");
        assertThatThrownBy(() -> timedOut.liftSuspension(SEVEN_DAYS_END))
                .isInstanceOf(CustomerNotSuspendedException.class);
        assertThatThrownBy(() -> alreadyLifted.liftSuspension(at(2026, 12, 4, 15, 0)))
                .isInstanceOf(CustomerNotSuspendedException.class);
        assertThat(alreadyLifted.suspension().orElseThrow().liftedAt()).contains(NEXT_DAY);
    }
}
