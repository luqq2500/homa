package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Percentage;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.ReservationValues.Version;
import com.homa.reservation.domain.terms.CancellationTier;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReservationValuesTest {

    private static final Version VALUES = ReservationValues.current();

    @Test
    @Tag("JOB-1")
    @DisplayName("§11: the Check-in Period is the 15 minutes before the job start time, and the Protection Window runs until 24 hours after the job end time.")
    void checkInPeriodAndProtectionWindowAreHomasValues() {
        assertThat(VALUES.checkInPeriod()).isEqualTo(Duration.ofMinutes(15));
        assertThat(VALUES.protectionAfterJobEnd()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("§11: the acceptance deadline is 24 hours after the request, or 1 hour before the Check-in Period, whichever is first.")
    void acceptanceDeadlineValuesAreHomas() {
        assertThat(VALUES.acceptanceResponseTime()).isEqualTo(Duration.ofHours(24));
        assertThat(VALUES.acceptanceCutOffBeforeCheckIn()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    @Tag("CCX-1")
    @DisplayName("§11: the Grace Period is 4 hours after Reserved, if Reserved at least 24 hours before the Check-in Period; the tiers are free until Flexible 4 hours, Standard 12 hours and Firm 24 hours before it.")
    void graceAndTierCutOffsAreHomasValues() {
        assertThat(VALUES.gracePeriod()).isEqualTo(Duration.ofHours(4));
        assertThat(VALUES.graceNeedsReservedBeforeCheckIn()).isEqualTo(Duration.ofHours(24));
        assertThat(VALUES.freeCancellationBeforeCheckIn()).containsOnly(
                Map.entry(CancellationTier.FLEXIBLE, Duration.ofHours(4)),
                Map.entry(CancellationTier.STANDARD, Duration.ofHours(12)),
                Map.entry(CancellationTier.FIRM, Duration.ofHours(24)));
    }

    @Test
    @Tag("PCX-2")
    @DisplayName("§11: provider fines are Base, Base + 10% within 24 hours, + 12% for no roster, + 20% last minute and + 30% for a no show; compensation is 3%, 4%, 7% and 10%.")
    void finesAndCompensationAreHomasValues() {
        assertThat(VALUES.providerFines()).isEqualTo(new ReservationValues.ProviderFines(Duration.ofHours(24),
                Percentage.of(10), Percentage.of(12), Percentage.of(20), Percentage.of(30)));
        assertThat(VALUES.compensation()).isEqualTo(new ReservationValues.CustomerCompensation(
                Percentage.of(3), Percentage.of(4), Percentage.of(7), Percentage.of(10)));
    }

    @Test
    @Tag("NSH-5")
    @DisplayName("§11: a Customer No Show pays 30% of the Hours Fee after the first hour; no fee or fine exceeds 100% of the Total Reservation Fee; worked time is charged in 15-minute steps.")
    void noShowShareCapAndPartialTimeStepAreHomasValues() {
        assertThat(VALUES.customerNoShowShareOfLaterHours()).isEqualTo(Percentage.of(30));
        assertThat(VALUES.feeAndFineCap()).isEqualTo(Percentage.HUNDRED_PERCENT);
        assertThat(VALUES.partialTimeStep()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @Tag("JOB-5")
    @DisplayName("§11: the provider is reminded at the job end time and 2 hours later.")
    void completionRemindersAreHomasValues() {
        assertThat(VALUES.completionRemindersAfterJobEnd()).containsExactly(Duration.ZERO, Duration.ofHours(2));
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Homa's first values are in force from the beginning, and they are the ones in force today.")
    void firstVersionIsInForceFromTheBeginning() {
        assertThat(ReservationValues.FIRST.effectiveFrom()).isEqualTo(Instant.EPOCH);
        assertThat(ReservationValues.asOf(Instant.EPOCH)).isEqualTo(ReservationValues.FIRST);
        assertThat(ReservationValues.asOf(REQUESTED_AT)).isEqualTo(ReservationValues.FIRST);
        assertThat(ReservationValues.current()).isEqualTo(ReservationValues.FIRST);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Homa lengthens the Check-in Period to 30 minutes from Tuesday 9:00. Aisyah's Saturday reservation, Requested on Monday 20:00, keeps its 9:45 Check-in Period; one Requested from Tuesday 9:00 gets 9:30.")
    void laterVersionLeavesAReservationRequestedEarlierOnTheOldVersion() {
        Version longerCheckIn = withCheckInPeriod(tuesday(9, 0), Duration.ofMinutes(30));
        ReservationValues history = new ReservationValues(List.of(longerCheckIn, ReservationValues.FIRST));
        JobTime saturdayJob = JobTime.of(JOB_START, STANDARD_HOURS);

        assertThat(history.versionAsOf(REQUESTED_AT)).isEqualTo(ReservationValues.FIRST);
        assertThat(history.versionAsOf(tuesday(8, 59))).isEqualTo(ReservationValues.FIRST);
        assertThat(history.versionAsOf(tuesday(9, 0))).isEqualTo(longerCheckIn);
        assertThat(history.latest()).isEqualTo(longerCheckIn);
        assertThat(saturdayJob.checkInStart(history.versionAsOf(REQUESTED_AT))).isEqualTo(saturday(9, 45));
        assertThat(saturdayJob.checkInStart(history.versionAsOf(tuesday(9, 0)))).isEqualTo(saturday(9, 30));

        Reservation aisyahs = Marketplace.withKemasHome().requestStandard();
        assertThat(aisyahs.values()).isEqualTo(ReservationValues.asOf(aisyahs.requestedAt()));
    }

    @Test
    @Tag("RES-8")
    @DisplayName("With three versions, a reservation reads the latest one in force when it was Requested, not the newest.")
    void reservationReadsTheLatestVersionInForceWhenRequested() {
        Version second = withCheckInPeriod(tuesday(9, 0), Duration.ofMinutes(20));
        Version third = withCheckInPeriod(saturday(0, 0), Duration.ofMinutes(30));
        ReservationValues history = new ReservationValues(List.of(ReservationValues.FIRST, third, second));

        assertThat(history.versionAsOf(tuesday(12, 0))).isEqualTo(second);
        assertThat(history.versionAsOf(saturday(0, 0))).isEqualTo(third);
        assertThat(history.latest()).isEqualTo(third);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("The values always start with a version in force from the beginning.")
    void valuesStartWithAVersionInForceFromTheBeginning() {
        assertThatThrownBy(() -> new ReservationValues(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReservationValues(List.of(withCheckInPeriod(tuesday(9, 0), Duration.ofMinutes(30)))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Version withCheckInPeriod(Instant effectiveFrom, Duration checkInPeriod) {
        Version first = ReservationValues.FIRST;
        return new Version(effectiveFrom, checkInPeriod, first.protectionAfterJobEnd(), first.acceptanceResponseTime(),
                first.acceptanceCutOffBeforeCheckIn(), first.gracePeriod(), first.graceNeedsReservedBeforeCheckIn(),
                first.freeCancellationBeforeCheckIn(), first.providerFines(), first.compensation(),
                first.customerNoShowShareOfLaterHours(), first.feeAndFineCap(), first.partialTimeStep(),
                first.completionRemindersAfterJobEnd());
    }
}
