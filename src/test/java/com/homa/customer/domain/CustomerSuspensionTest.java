package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.DANIEL_SUSPENDED_AT;
import static com.homa.customer.domain.CustomerFixtures.SAFETY_AND_MISCONDUCT;
import static com.homa.customer.domain.CustomerFixtures.daniel;
import static com.homa.customer.domain.CustomerFixtures.danielSuspended;
import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.exception.CustomerAlreadySuspendedException;
import com.homa.customer.domain.exception.CustomerNotSuspendedException;
import com.homa.customer.domain.exception.MissingSuspensionPeriodException;
import com.homa.customer.domain.exception.MissingSuspensionReasonException;
import com.homa.customer.domain.exception.SuspensionEndsOnItsOwnException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerSuspensionTest {

    /** Daniel's suspension for the longer set period runs out on Friday 1 January 2027 at 15:00. */
    private static final Instant THIRTY_DAYS_END = at(2027, 1, 1, 15, 0);
    private static final Instant NEXT_DAY = at(2026, 12, 3, 15, 0);

    @Test
    @Tag("CUS-4")
    @DisplayName("Kemas Home reports that a customer, Daniel, shouted at and threatened its helpers. Support confirms "
            + "the report and suspends Daniel for 30 days.")
    void supportSuspendsACustomerForThirtyDaysAfterAConfirmedSafetyAndMisconductReport() {
        Customer daniel = danielSuspended(SuspensionPeriod.THIRTY_DAYS);

        Suspension suspension = daniel.suspension().orElseThrow();
        assertThat(suspension.period()).isEqualTo(SuspensionPeriod.THIRTY_DAYS);
        assertThat(suspension.reason()).isEqualTo(SAFETY_AND_MISCONDUCT);
        assertThat(suspension.suspendedAt()).isEqualTo(DANIEL_SUSPENDED_AT);
        assertThat(suspension.endsAt()).contains(THIRTY_DAYS_END);
        assertThat(suspension.liftedAt()).isEmpty();
        assertThat(daniel.isSuspendedAt(DANIEL_SUSPENDED_AT)).isTrue();
        assertThat(daniel.statusAt(NEXT_DAY)).isEqualTo(CustomerStatus.SUSPENDED);
        assertThat(daniel.canMakeReservationsAt(NEXT_DAY)).isFalse();
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("Support can instead suspend Daniel for the shorter set period, or until Support lifts it.")
    void supportSuspendsACustomerForTheShorterPeriodOrUntilLifted() {
        Customer shorter = danielSuspended(SuspensionPeriod.SEVEN_DAYS);
        Customer untilLifted = danielSuspended(SuspensionPeriod.UNTIL_LIFTED);

        assertThat(shorter.suspension().orElseThrow().endsAt()).contains(at(2026, 12, 9, 15, 0));
        assertThat(shorter.canMakeReservationsAt(NEXT_DAY)).isFalse();
        assertThat(untilLifted.suspension().orElseThrow().endsAt()).isEmpty();
        assertThat(untilLifted.canMakeReservationsAt(NEXT_DAY)).isFalse();
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("Support must say why and for how long it suspends Daniel, and can't suspend him again while his "
            + "suspension is in force.")
    void supportSuspendsACustomerOnlyWithAReasonAndAPeriodAndOnlyOnceAtATime() {
        Customer daniel = daniel();

        assertThatThrownBy(() -> daniel.suspend(SuspensionPeriod.THIRTY_DAYS, null, DANIEL_SUSPENDED_AT))
                .isInstanceOf(MissingSuspensionReasonException.class);
        assertThatThrownBy(() -> daniel.suspend(SuspensionPeriod.THIRTY_DAYS, " ", DANIEL_SUSPENDED_AT))
                .isInstanceOf(MissingSuspensionReasonException.class)
                .hasMessageContaining("CUS-4");
        assertThatThrownBy(() -> daniel.suspend(null, SAFETY_AND_MISCONDUCT, DANIEL_SUSPENDED_AT))
                .isInstanceOf(MissingSuspensionPeriodException.class)
                .hasMessage("CUS-4: Support must choose how long the suspension lasts");
        assertThat(daniel.suspension()).isEmpty();
        assertThat(daniel.statusAt(DANIEL_SUSPENDED_AT)).isEqualTo(CustomerStatus.ACTIVE);

        daniel.suspend(SuspensionPeriod.THIRTY_DAYS, SAFETY_AND_MISCONDUCT, DANIEL_SUSPENDED_AT);

        assertThatThrownBy(() -> daniel.suspend(SuspensionPeriod.UNTIL_LIFTED, "Repeated confirmed no-shows",
                THIRTY_DAYS_END.minusNanos(1)))
                .isInstanceOf(CustomerAlreadySuspendedException.class)
                .hasMessage("CUS-4: customer " + daniel.id().value() + " is already suspended");
        assertThat(daniel.suspension().orElseThrow().reason()).isEqualTo(SAFETY_AND_MISCONDUCT);
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("After Daniel's suspension has ended, Support can suspend him again for a new report.")
    void supportSuspendsACustomerAgainOnceTheEarlierSuspensionHasEnded() {
        Customer daniel = danielSuspended(SuspensionPeriod.SEVEN_DAYS);
        Instant later = at(2026, 12, 20, 9, 0);

        daniel.suspend(SuspensionPeriod.UNTIL_LIFTED, "Repeated confirmed no-shows", later);

        Suspension again = daniel.suspension().orElseThrow();
        assertThat(again.period()).isEqualTo(SuspensionPeriod.UNTIL_LIFTED);
        assertThat(again.reason()).isEqualTo("Repeated confirmed no-shows");
        assertThat(again.suspendedAt()).isEqualTo(later);
        assertThat(daniel.isSuspendedAt(later)).isTrue();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Daniel's 30-day suspension ends on its own, and he can book again.")
    void aSuspensionForASetPeriodEndsOnItsOwnWhenThePeriodRunsOut() {
        Customer daniel = danielSuspended(SuspensionPeriod.THIRTY_DAYS);
        Suspension suspension = daniel.suspension().orElseThrow();

        assertThat(daniel.isSuspendedAt(THIRTY_DAYS_END.minusNanos(1))).isTrue();
        assertThat(daniel.canMakeReservationsAt(THIRTY_DAYS_END.minusNanos(1))).isFalse();

        assertThat(daniel.isSuspendedAt(THIRTY_DAYS_END)).isFalse();
        assertThat(daniel.canMakeReservationsAt(THIRTY_DAYS_END)).isTrue();
        assertThat(daniel.statusAt(THIRTY_DAYS_END)).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(daniel.suspension()).containsSame(suspension);
        assertThat(suspension.liftedAt()).isEmpty();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("A customer suspended until lifted must wait for Support to review an appeal.")
    void aSuspensionUntilLiftedEndsOnlyWhenSupportLiftsIt() {
        Customer daniel = danielSuspended(SuspensionPeriod.UNTIL_LIFTED);
        Instant appealReviewed = at(2027, 3, 15, 11, 0);

        assertThat(daniel.isSuspendedAt(at(2027, 3, 1, 0, 0))).isTrue();

        daniel.liftSuspension(appealReviewed);

        Suspension lifted = daniel.suspension().orElseThrow();
        assertThat(lifted.liftedAt()).contains(appealReviewed);
        assertThat(lifted.endsAt()).isEmpty();
        assertThat(lifted.period()).isEqualTo(SuspensionPeriod.UNTIL_LIFTED);
        assertThat(lifted.reason()).isEqualTo(SAFETY_AND_MISCONDUCT);
        assertThat(lifted.suspendedAt()).isEqualTo(DANIEL_SUSPENDED_AT);
        assertThat(daniel.isSuspendedAt(appealReviewed.minusNanos(1))).isTrue();
        assertThat(daniel.isSuspendedAt(appealReviewed)).isFalse();
        assertThat(daniel.canMakeReservationsAt(appealReviewed)).isTrue();
        assertThat(daniel.statusAt(appealReviewed)).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Support can't end Daniel's 30-day suspension early; it ends on its own when the period runs out.")
    void supportCannotLiftASuspensionForASetPeriod() {
        Customer daniel = danielSuspended(SuspensionPeriod.THIRTY_DAYS);

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
        Customer timedOut = danielSuspended(SuspensionPeriod.SEVEN_DAYS);
        Customer alreadyLifted = danielSuspended(SuspensionPeriod.UNTIL_LIFTED);
        alreadyLifted.liftSuspension(NEXT_DAY);

        assertThatThrownBy(() -> neverSuspended.liftSuspension(NEXT_DAY))
                .isInstanceOf(CustomerNotSuspendedException.class)
                .hasMessage("CUS-6: customer " + neverSuspended.id().value()
                        + " is not suspended, so there is no suspension to lift");
        assertThatThrownBy(() -> timedOut.liftSuspension(at(2026, 12, 9, 15, 0)))
                .isInstanceOf(CustomerNotSuspendedException.class);
        assertThatThrownBy(() -> alreadyLifted.liftSuspension(at(2026, 12, 4, 15, 0)))
                .isInstanceOf(CustomerNotSuspendedException.class);
        assertThat(alreadyLifted.suspension().orElseThrow().liftedAt()).contains(NEXT_DAY);
    }
}
