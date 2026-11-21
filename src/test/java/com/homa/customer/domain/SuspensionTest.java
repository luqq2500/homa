package com.homa.customer.domain;

import static com.homa.customer.domain.CustomerFixtures.SAFETY_AND_MISCONDUCT;
import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.BusinessCalendar;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SuspensionTest {

    private static Suspension suspendedAt(SuspensionPeriod period, Instant now) {
        return Suspension.start(period, SAFETY_AND_MISCONDUCT, now);
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("Daniel, suspended for the shorter period on Wednesday 2 December at 15:00, is still suspended the "
            + "instant before Wednesday 9 December at 15:00 and free at 15:00.")
    void aShortSuspensionEndsAtTheSameKualaLumpurTimeAfterItsCalendarDays() {
        Suspension shorter = suspendedAt(SuspensionPeriod.SEVEN_DAYS, at(2026, 12, 2, 15, 0));
        Instant endsAt = at(2026, 12, 9, 15, 0);

        assertThat(shorter.endsAt()).contains(endsAt);
        assertThat(shorter.isInForceAt(endsAt.minusNanos(1))).isTrue();
        assertThat(shorter.isInForceAt(endsAt)).isFalse();
        assertThat(shorter.isInForceAt(endsAt.plusSeconds(1))).isFalse();
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("A suspension for the longer period counts calendar days, not a calendar month: from 2 December it "
            + "ends on 1 January.")
    void aLongSuspensionCountsCalendarDaysAcrossTheMonthEnd() {
        Suspension longer = suspendedAt(SuspensionPeriod.THIRTY_DAYS, at(2026, 12, 2, 15, 0));
        Instant endsAt = at(2027, 1, 1, 15, 0);

        assertThat(longer.endsAt()).contains(endsAt);
        assertThat(longer.isInForceAt(endsAt.minusNanos(1))).isTrue();
        assertThat(longer.isInForceAt(endsAt)).isFalse();
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("Suspended at 00:15 on 3 December in Kuala Lumpur, which is still 2 December in UTC, Daniel's "
            + "shorter suspension ends at 00:15 on 10 December in Kuala Lumpur.")
    void suspensionDaysAreCountedOnTheKualaLumpurCalendar() {
        Instant justAfterMidnight = at(2026, 12, 3, 0, 15);
        Suspension shorter = suspendedAt(SuspensionPeriod.SEVEN_DAYS, justAfterMidnight);
        Instant endsAt = at(2026, 12, 10, 0, 15);

        assertThat(BusinessCalendar.dateOf(justAfterMidnight)).isEqualTo(LocalDate.of(2026, 12, 3));
        assertThat(shorter.endsAt()).contains(endsAt);
        assertThat(BusinessCalendar.dateOf(shorter.endsAt().orElseThrow())).isEqualTo(LocalDate.of(2026, 12, 10));
        assertThat(shorter.isInForceAt(at(2026, 12, 9, 23, 59))).isTrue();
        assertThat(shorter.isInForceAt(at(2026, 12, 10, 0, 0))).isTrue();
        assertThat(shorter.isInForceAt(endsAt)).isFalse();
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("Suspended at 23:45 on 2 December in Kuala Lumpur, Daniel's shorter suspension ends at 23:45 on "
            + "9 December, not at the start of a day.")
    void aSuspensionLateInTheKualaLumpurDayEndsLateInTheDay() {
        Suspension shorter = suspendedAt(SuspensionPeriod.SEVEN_DAYS, at(2026, 12, 2, 23, 45));
        Instant endsAt = at(2026, 12, 9, 23, 45);

        assertThat(shorter.endsAt()).contains(endsAt);
        assertThat(shorter.isInForceAt(at(2026, 12, 9, 0, 0))).isTrue();
        assertThat(shorter.isInForceAt(endsAt.minusNanos(1))).isTrue();
        assertThat(shorter.isInForceAt(endsAt)).isFalse();
    }

    @Test
    @Tag("CUS-4")
    @DisplayName("A suspension takes effect the moment Support confirms it, not before.")
    void aSuspensionIsInForceFromTheMomentItStarts() {
        Instant confirmed = at(2026, 12, 2, 15, 0);

        for (SuspensionPeriod period : SuspensionPeriod.values()) {
            Suspension suspension = suspendedAt(period, confirmed);
            assertThat(suspension.isInForceAt(confirmed.minusNanos(1))).as(period.name()).isFalse();
            assertThat(suspension.isInForceAt(confirmed)).as(period.name()).isTrue();
        }
    }

    @Test
    @Tag("CUS-6")
    @DisplayName("A suspension until lifted never runs out on its own.")
    void aSuspensionUntilLiftedHasNoEndOfItsOwn() {
        Suspension untilLifted = suspendedAt(SuspensionPeriod.UNTIL_LIFTED, at(2026, 12, 2, 15, 0));

        assertThat(untilLifted.endsAt()).isEmpty();
        assertThat(untilLifted.liftedAt()).isEmpty();
        assertThat(untilLifted.isInForceAt(at(2036, 12, 2, 15, 0))).isTrue();
    }
}
