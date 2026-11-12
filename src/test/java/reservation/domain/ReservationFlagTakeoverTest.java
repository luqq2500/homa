package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.A_PHOTO;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;

import java.util.List;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.InvalidStatusTransitionException;

/** Feature: A flag takes over a job dispute. Background: Saturday 10:00 to 13:00, total RM230. */
class ReservationFlagTakeoverTest {

    @Test
    void aDisputedJobWithNoCompletionIsTakenOver() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "Only one helper came", List.of(), saturday("10:30"));

        reservation.disputeForProviderFlag(saturday("11:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reservation.ending()).contains(Ending.PROVIDER_FLAGGED);
        assertThat(reservation.reports()).hasSize(1);
        Settlement settlement = reservation.cancelForProviderFlag(saturday("11:30"));
        assertThat(settlement.customerRefund()).isEqualTo(rm("230.00"));
    }

    @Test
    void aDisputedJobWithCompletionIsNotTakenOver() {
        Reservation reservation = aReservation().completedAt(saturday("12:45"));
        reservation.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(A_PHOTO), saturday("13:30"));

        assertThatThrownBy(() -> reservation.disputeForProviderFlag(saturday("13:40")))
                .isInstanceOf(CompletionAlreadySubmittedException.class);
        assertThat(reservation.ending()).isEmpty();
    }

    @Test
    void aChallengedNoShowIsNotTakenOver() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.reportCustomerNoShow(saturday("10:15"));
        reservation.challengeNoShow(saturday("10:45"));

        assertThatThrownBy(() -> reservation.disputeForProviderFlag(saturday("11:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(reservation.ending()).contains(Ending.CUSTOMER_NO_SHOW);
    }
}
