package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.A_PHOTO;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.sunday;

import java.util.List;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.ReportWindowClosedException;

/** Feature: Completion while disputed over the job. Background: Saturday 10:00 to 13:00, checked in at 10:00. */
class ReservationCompletionDuringDisputeTest {

    @Test
    void theProviderSubmitsCompletionAfterTheCustomerReports() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "Only one helper came", List.of(), saturday("11:00"));

        reservation.submitCompletion(List.of(A_PHOTO), saturday("12:45"));

        assertThat(reservation.completion().orElseThrow().at()).isEqualTo(saturday("12:45"));
        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reservation.disputeWindowEndsAt()).contains(saturday("14:00"));
        reservation.fileReport(Party.PROVIDER, ReportType.DURING_JOB, "The customer asked for extra rooms", List.of(), saturday("14:00").minusSeconds(1));
        assertThat(reservation.reports()).hasSize(2);
    }

    @Test
    void theOtherPartyCannotReportAfterTheWindowEnds() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "Only one helper came", List.of(), saturday("11:00"));
        reservation.submitCompletion(List.of(A_PHOTO), saturday("12:45"));

        assertThatThrownBy(() -> reservation.fileReport(Party.PROVIDER, ReportType.DURING_JOB, "Late", List.of(), saturday("14:00")))
                .isInstanceOf(ReportWindowClosedException.class);
    }

    @Test
    void theProviderSubmitsCompletionAfterMissingTheDeadline() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.missCompletionDeadline(sunday("13:00"));

        reservation.submitCompletion(List.of(A_PHOTO), sunday("14:00"));

        assertThat(reservation.completion()).isPresent();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
    }

    @Test
    void completionIsStillSubmittedOnce() {
        Reservation reservation = aReservation().completedAt(saturday("12:45"));
        reservation.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(A_PHOTO), saturday("13:30"));

        assertThatThrownBy(() -> reservation.submitCompletion(List.of(A_PHOTO), saturday("13:40")))
                .isInstanceOf(CompletionAlreadySubmittedException.class);
    }

    @Test
    void aChallengedNoShowOrAFlaggedReservationTakesNoCompletion() {
        Reservation noShow = aReservation().checkedInAt(saturday("10:00"));
        noShow.reportCustomerNoShow(saturday("10:15"));
        noShow.challengeNoShow(saturday("10:45"));
        Reservation flagged = aReservation().checkedInAt(saturday("10:00"));
        flagged.disputeForProviderFlag(saturday("11:00"));

        assertThatThrownBy(() -> noShow.submitCompletion(List.of(A_PHOTO), saturday("12:45")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> flagged.submitCompletion(List.of(A_PHOTO), saturday("12:45")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
