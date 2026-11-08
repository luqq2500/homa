package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.A_PHOTO;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.sunday;

import java.util.List;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.ReportAlreadyFiledException;
import reservation.domain.exception.ReportWindowClosedException;
import reservation.domain.exception.ReservationDetailRequiredException;

/** Feature: Reports. Background: reserved for Saturday 10:00 to 13:00, completion submitted at 12:45. */
class ReservationReportTest {

    private static Reservation completedAt1245() {
        return aReservation().completedAt(saturday("12:45"));
    }

    @Test
    void theCustomerFilesAReportInsideTheWindow() {
        Reservation reservation = completedAt1245();

        reservation.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left on the counter", List.of(A_PHOTO), saturday("13:30"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reservation.reports()).containsExactly(new Report(
                Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left on the counter", List.of(A_PHOTO), saturday("13:30")));
    }

    @Test
    void theProviderCanFileAReportToo() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

        reservation.fileReport(Party.PROVIDER, ReportType.DURING_JOB, "No water supply", List.of(), saturday("11:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
    }

    @Test
    void eachPartyFilesAtMostOneReport() {
        Reservation reservation = completedAt1245();
        reservation.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(A_PHOTO), saturday("13:30"));

        assertThatThrownBy(() -> reservation.fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "Late", List.of(), saturday("13:40")))
                .isInstanceOf(ReportAlreadyFiledException.class)
                .hasMessageContaining("CUSTOMER");
    }

    @Test
    void theOtherPartyCanStillReportWhileDisputed() {
        Reservation reservation = completedAt1245();
        reservation.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(A_PHOTO), saturday("13:30"));

        reservation.fileReport(Party.PROVIDER, ReportType.FINISHED_WORK, "Customer refused entry to a room", List.of(), saturday("13:45"));

        assertThat(reservation.reports()).hasSize(2);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
    }

    @Test
    void reportsBeforeTheBookedStartAreRejected() {
        assertThatThrownBy(() -> aReservation().reservedWithTeamNamed()
                        .fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "Wrong helper", List.of(), saturday("09:59")))
                .isInstanceOf(ReportWindowClosedException.class);
    }

    @Test
    void reportsOpenAtTheBookedStart() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        reservation.fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "Wrong helper", List.of(), saturday("10:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
    }

    @Test
    void reportsAfterTheWindowAreRejected() {
        assertThatThrownBy(() -> completedAt1245()
                        .fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(), saturday("14:01")))
                .isInstanceOf(ReportWindowClosedException.class);
        assertThatThrownBy(() -> completedAt1245()
                        .fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(), saturday("14:00")))
                .isInstanceOf(ReportWindowClosedException.class);
        Reservation lastMinute = completedAt1245();
        lastMinute.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(), saturday("13:59:59"));
        assertThat(lastMinute.status()).isEqualTo(ReservationStatus.DISPUTED);
    }

    @Test
    void aReportNeedsAReason() {
        assertThatThrownBy(() -> completedAt1245().fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, " ", List.of(A_PHOTO), saturday("13:30")))
                .isInstanceOf(ReservationDetailRequiredException.class);
    }

    @Test
    void aMissedCompletionCanStillBeReported() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.missCompletionDeadline(sunday("13:00"));

        reservation.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "The job was never finished", List.of(), sunday("13:30"));

        assertThat(reservation.reports()).hasSize(1);
    }

    @Test
    void aChallengedNoShowTakesNoReports() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.reportCustomerNoShow(saturday("10:15"));
        reservation.challengeNoShow(saturday("10:30"));

        assertThatThrownBy(() -> reservation.fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "I was home", List.of(), saturday("10:40")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void aRequestOrADeclinedRequestTakesNoReports() {
        assertThatThrownBy(() -> aReservation().requested()
                        .fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "No one came", List.of(), saturday("10:30")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        Reservation declined = aReservation().requested();
        declined.decline(ReservationBuilders.thursday("12:00"));
        assertThatThrownBy(() -> declined.fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "No one came", List.of(), saturday("10:30")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void anEndedReservationTakesNoReports() {
        Reservation reservation = completedAt1245();
        reservation.closeDisputeWindow(saturday("14:00"));

        assertThatThrownBy(() -> reservation.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains", List.of(), saturday("14:05")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
