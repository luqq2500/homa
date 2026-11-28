package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.reservation.domain.ReservationFixtures.friday;
import static com.homa.reservation.domain.ReservationFixtures.nurul;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static com.homa.reservation.domain.ReservationFixtures.thursday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.CheckInPeriodNotStartedException;
import com.homa.reservation.domain.exception.JobAlreadyEndedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.exception.RosterLockedException;
import com.homa.reservation.domain.settlement.Settlement;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** ROS-1, ROS-7, NRS-1 to NRS-3: the roster locks when the Check-in Period starts, or the No Roster Policy applies. */
class ReservationNoRosterTest {

    private static final Settlement NO_ROSTER_ON_THE_STANDARD_RESERVATION =
            new Settlement(Money.myr(0), Money.myr(200), Money.myr(8), Money.myr(69), Money.myr(0));

    private final Reservation reservation = Marketplace.withKemasHome().reserveStandard();

    private void rosteredWithNurulNoLongerActive() {
        reservation.assignRoster(List.of(siti(), nurul()), thursday(9, 0));
        reservation.rosterMemberStoppedBeingActive(NURUL_MEMBER_ID, friday(10, 0));
    }

    @Test
    @Tag("ROS-7")
    @Tag("NRS-1")
    @Tag("NRS-2")
    @Tag("NRS-3")
    @DisplayName("Nurul is suspended on Friday and not replaced by 9:45. The No Roster Policy applies: fine RM69, Aisyah gets RM208.")
    void anIncompleteRosterAtCheckInIsCancelledUnderTheNoRosterPolicy() {
        rosteredWithNurulNoLongerActive();

        Optional<Cancellation> cancelled = reservation.lockRosterAtCheckIn(saturday(9, 45));

        assertThat(cancelled).contains(
                new Cancellation(saturday(9, 45), CancellationCause.NO_ROSTER, NO_ROSTER_ON_THE_STANDARD_RESERVATION));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.cancellation()).isEqualTo(cancelled);
        assertThat(reservation.roster().orElseThrow().isLocked()).isFalse();
    }

    @Test
    @Tag("NRS-1")
    @DisplayName("At 9:45 only Siti is rostered for 2 helpers, or nobody is: the booking is cancelled and Aisyah is told at once.")
    void aReservationWithNoRosterAtAllIsCancelledAtCheckIn() {
        Optional<Cancellation> cancelled = reservation.lockRosterAtCheckIn(saturday(9, 45));

        assertThat(cancelled).contains(
                new Cancellation(saturday(9, 45), CancellationCause.NO_ROSTER, NO_ROSTER_ON_THE_STANDARD_RESERVATION));
        assertThat(reservation.roster()).isEmpty();
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Kemas Home rosters Siti and Nurul, and the roster locks at 9:45 with them on it and the reservation goes on.")
    void aCompleteRosterLocksAtCheckInAndTheReservationGoesOn() {
        reservation.assignRoster(List.of(siti(), nurul()), friday(18, 0));

        Optional<Cancellation> cancelled = reservation.lockRosterAtCheckIn(saturday(9, 45));

        assertThat(cancelled).isEmpty();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.cancellation()).isEmpty();
        assertThat(reservation.roster().orElseThrow().lockedAt()).contains(saturday(9, 45));
        assertThat(reservation.roster().orElseThrow().members()).hasSize(2);
        assertThatThrownBy(() -> reservation.assignRoster(List.of(siti(), nurul()), saturday(9, 46)))
                .isInstanceOf(RosterLockedException.class);
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("At 9:44 the roster can still change, so it isn't locked and the No Roster Policy can't apply yet.")
    void theRosterIsNotLockedBeforeTheCheckInPeriod() {
        assertThatThrownBy(() -> reservation.lockRosterAtCheckIn(saturday(9, 44)))
                .isInstanceOf(CheckInPeriodNotStartedException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);

        reservation.assignRoster(List.of(siti(), nurul()), saturday(9, 44));

        assertThat(reservation.roster().orElseThrow().isLocked()).isFalse();
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("A sweep that comes at 9:46 still finds Nurul not replaced: the reservation is cancelled as of 9:46.")
    void aLateSweepStillAppliesTheNoRosterPolicy() {
        rosteredWithNurulNoLongerActive();

        Cancellation cancelled = reservation.lockRosterAtCheckIn(saturday(9, 46)).orElseThrow();

        assertThat(cancelled.at()).isEqualTo(saturday(9, 46));
        assertThat(cancelled.cause()).isEqualTo(CancellationCause.NO_ROSTER);
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("Once the job has ended there is no roster to lock: at 13:59 it still applies, at 14:00 it is refused.")
    void aJobThatEndedHasNoRosterToLock() {
        assertThatThrownBy(() -> reservation.lockRosterAtCheckIn(saturday(14, 0)))
                .isInstanceOf(JobAlreadyEndedException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);

        assertThat(reservation.lockRosterAtCheckIn(saturday(13, 59))).isPresent();
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("A roster locks once: locking it again, or after the reservation was cancelled, is refused.")
    void aRosterThatIsLockedOrCancelledCannotBeLockedAgain() {
        reservation.assignRoster(List.of(siti(), nurul()), friday(18, 0));
        reservation.lockRosterAtCheckIn(saturday(9, 45));

        assertThatThrownBy(() -> reservation.lockRosterAtCheckIn(saturday(9, 46)))
                .isInstanceOf(RosterLockedException.class);
        assertThat(reservation.roster().orElseThrow().lockedAt()).contains(saturday(9, 45));

        Reservation cancelled = Marketplace.withKemasHome().reserveStandard();
        cancelled.lockRosterAtCheckIn(saturday(9, 45));
        assertThatThrownBy(() -> cancelled.lockRosterAtCheckIn(saturday(9, 46)))
                .isInstanceOf(ReservationStatusConflictException.class);
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("A request nobody accepted yet has no roster to lock.")
    void aRequestedReservationHasNoRosterToLock() {
        Reservation requested = Marketplace.withKemasHome().requestStandard();

        assertThatThrownBy(() -> requested.lockRosterAtCheckIn(saturday(9, 45)))
                .isInstanceOf(ReservationStatusConflictException.class);
    }

    @Test
    @Tag("NRS-2")
    @DisplayName("Kemas Home is fined RM69: the Base Cancellation Fee RM45 and 12% of RM200.")
    void theProviderIsFinedBasePlusTwelvePercent() {
        Settlement settlement = reservation.lockRosterAtCheckIn(saturday(9, 45)).orElseThrow().settlement();

        assertThat(settlement.providerFine()).isEqualTo(Money.myr(69));
        assertThat(settlement.customerFee()).isEqualTo(Money.myr(0));
        assertThat(settlement.providerPayoutGross()).isEqualTo(Money.myr(0));
    }

    @Test
    @Tag("NRS-3")
    @DisplayName("Aisyah gets RM208: her RM200 back and 4% of it, RM8, from Homa.")
    void theCustomerGetsAFullRefundAndFourPercent() {
        Settlement settlement = reservation.lockRosterAtCheckIn(saturday(9, 45)).orElseThrow().settlement();

        assertThat(settlement.customerRefund()).isEqualTo(Money.myr(200));
        assertThat(settlement.compensation()).isEqualTo(Money.myr(8));
        assertThat(settlement.customerRefund().plus(settlement.compensation())).isEqualTo(Money.myr(208));
    }
}
