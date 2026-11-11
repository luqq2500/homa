package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.A_PHOTO;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;

import java.util.List;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.FlaggedReservationStartedException;
import reservation.domain.exception.InvalidStatusTransitionException;

/**
 * Feature: A seriously flagged provider. The KYC provider found a serious flag on Kemas, such as being struck
 * off; Kemas's upcoming accepted reservations are disputed and cancelled with a full refund.
 */
class ReservationProviderFlagTest {

    @Test
    void anAcceptedReservationIsDisputedWhenTheProviderIsFlagged() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        reservation.disputeForProviderFlag(friday("12:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reservation.ending()).contains(Ending.PROVIDER_FLAGGED);
    }

    @Test
    void aFlaggedReservationIsCancelledWithAFullRefundAndNoFine() {
        Reservation reservation = aReservation().reservedWithTeamNamed();
        reservation.disputeForProviderFlag(friday("12:00"));

        Settlement settlement = reservation.cancelForProviderFlag(friday("14:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(settlement.ending()).isEqualTo(Ending.PROVIDER_FLAGGED);
        assertThat(settlement.customerRefund()).isEqualTo(rm("230.00"));
        assertThat(settlement.providerEarning()).isEqualTo(rm("0.00"));
    }

    @Test
    void aFlaggedReservationIsCancelledOnce() {
        Reservation reservation = aReservation().reservedWithTeamNamed();
        reservation.disputeForProviderFlag(friday("12:00"));
        reservation.cancelForProviderFlag(friday("14:00"));

        assertThatThrownBy(() -> reservation.cancelForProviderFlag(friday("15:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void aFlaggedReservationCannotBeCheckedIntoOrReported() {
        Reservation reservation = aReservation().reservedWithTeamNamed();
        reservation.disputeForProviderFlag(friday("12:00"));

        assertThatThrownBy(() -> reservation.checkIn(50, saturday("09:58")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> reservation.fileReport(
                        Party.CUSTOMER, ReportType.DURING_JOB, "The provider was flagged", List.of(), saturday("10:30")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void aFlaggedReservationIsNotResolvedAnyOtherWay() {
        Reservation reservation = aReservation().reservedWithTeamNamed();
        reservation.disputeForProviderFlag(friday("12:00"));

        assertThatThrownBy(() -> reservation.resolve(CaseDecision.release(), friday("14:00")))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("full refund");
        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
    }

    @Test
    void aJobUnderWayStopsWhenTheProviderIsFlagged() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

        reservation.disputeForProviderFlag(saturday("11:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThatThrownBy(() -> reservation.submitCompletion(List.of(A_PHOTO), saturday("13:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void aClearedFlagRestoresTheReservationBeforeItsStart() {
        Reservation reservation = aReservation().reservedWithTeamNamed();
        reservation.disputeForProviderFlag(friday("12:00"));

        reservation.restoreAfterFlagCleared(saturday("09:59"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.ending()).isEmpty();
        reservation.checkIn(50, saturday("10:00"));
        assertThat(reservation.checkInRecord()).isPresent();
    }

    @Test
    void aFlaggedReservationIsNotRestoredFromItsStart() {
        Reservation reservation = aReservation().reservedWithTeamNamed();
        reservation.disputeForProviderFlag(friday("12:00"));

        assertThatThrownBy(() -> reservation.restoreAfterFlagCleared(saturday("10:00")))
                .isInstanceOf(FlaggedReservationStartedException.class)
                .hasMessageContaining(saturday("10:00").toString());
        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
    }

    @Test
    void onlyAFlaggedReservationIsRestored() {
        Reservation reserved = aReservation().reservedWithTeamNamed();
        Reservation cancelled = aReservation().reservedWithTeamNamed();
        cancelled.disputeForProviderFlag(friday("12:00"));
        cancelled.cancelForProviderFlag(friday("13:00"));

        assertThatThrownBy(() -> reserved.restoreAfterFlagCleared(friday("12:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> cancelled.restoreAfterFlagCleared(friday("14:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void anUndecidedFlaggedReservationIsCancelledAtItsStart() {
        Reservation reservation = aReservation().reservedWithTeamNamed();
        reservation.disputeForProviderFlag(friday("12:00"));

        assertThat(reservation.isFlagCancellationDue(saturday("09:59"))).isFalse();
        assertThat(reservation.isFlagCancellationDue(saturday("10:00"))).isTrue();

        reservation.cancelForProviderFlag(saturday("10:00"));

        assertThat(reservation.isFlagCancellationDue(saturday("10:01"))).isFalse();
        assertThat(aReservation().reservedWithTeamNamed().isFlagCancellationDue(saturday("10:00"))).isFalse();
    }

    @Test
    void aReservationDisputedForAReportIsNotCancelledForAFlag() {
        Reservation reported = aReservation().completedAt(saturday("13:00"));
        reported.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(A_PHOTO), saturday("13:30"));

        assertThat(reported.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reported.isFlagCancellationDue(saturday("13:40"))).isFalse();
    }

    @Test
    void onlyAnAcceptedReservationIsDisputedForAFlag() {
        assertThatThrownBy(() -> aReservation().requested().disputeForProviderFlag(friday("12:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void aCompletedJobIsNotDisputedForAFlag() {
        Reservation reservation = aReservation().completedAt(saturday("13:00"));

        assertThatThrownBy(() -> reservation.disputeForProviderFlag(saturday("13:10")))
                .isInstanceOf(CompletionAlreadySubmittedException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void onlyAFlaggedReservationIsCancelledForAFlag() {
        Reservation reserved = aReservation().reservedWithTeamNamed();
        Reservation otherDispute = aReservation().completedAt(saturday("13:00"));
        otherDispute.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(A_PHOTO), saturday("13:30"));

        assertThatThrownBy(() -> reserved.cancelForProviderFlag(friday("14:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> otherDispute.cancelForProviderFlag(saturday("13:40")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(otherDispute.status()).isEqualTo(ReservationStatus.DISPUTED);
    }
}
