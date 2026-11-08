package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reservation.domain.exception.CompletionNotSubmittedException;
import reservation.domain.exception.DisputeWindowStillOpenException;
import reservation.domain.exception.InvalidStatusTransitionException;

/** Feature: Dispute window and payout. Background: reserved for Saturday 10:00 to 13:00, provider price RM200, total RM230. */
class ReservationDisputeWindowTest {

    static Stream<Arguments> windows() {
        return Stream.of(
                Arguments.of(saturday("12:45"), saturday("14:00")),
                Arguments.of(saturday("13:00"), saturday("14:00")),
                Arguments.of(saturday("13:40"), saturday("14:40")));
    }

    @ParameterizedTest(name = "completion at {0}: the window ends at {1}")
    @MethodSource("windows")
    void theWindowEndsAnHourAfterTheLaterOfTheBookedEndAndCompletion(Instant submitted, Instant ends) {
        assertThat(aReservation().completedAt(submitted).disputeWindowEndsAt()).contains(ends);
    }

    @Test
    void theWindowHasNoEndUntilCompletionIsSubmitted() {
        assertThat(aReservation().checkedInAt(saturday("10:00")).disputeWindowEndsAt()).isEmpty();
    }

    @Test
    void theWindowClosesWithNoReportAndThePaymentIsReleased() {
        Reservation reservation = aReservation().completedAt(saturday("12:45"));

        Settlement settlement = reservation.closeDisputeWindow(saturday("14:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(reservation.ending()).contains(Ending.COMPLETED);
        assertThat(settlement.ending()).isEqualTo(Ending.COMPLETED);
        assertThat(settlement.providerEarning()).isEqualTo(rm("200.00"));
        assertThat(settlement.serviceFee()).isEqualTo(rm("30.00"));
        assertThat(settlement.customerRefund()).isEqualTo(rm("0.00"));
        assertThat(settlement.releaseAt()).isEqualTo(saturday("14:00"));
    }

    @Test
    void theWindowCannotCloseEarly() {
        Reservation reservation = aReservation().completedAt(saturday("12:45"));

        assertThatThrownBy(() -> reservation.closeDisputeWindow(saturday("13:59")))
                .isInstanceOf(DisputeWindowStillOpenException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void theWindowCannotCloseBeforeCompletion() {
        assertThatThrownBy(() -> aReservation().checkedInAt(saturday("10:00")).closeDisputeWindow(saturday("15:00")))
                .isInstanceOf(CompletionNotSubmittedException.class);
    }

    @Test
    void aReportStopsTheRelease() {
        Reservation reservation = aReservation().completedAt(saturday("12:45"));
        reservation.fileReport(Party.CUSTOMER, ReportType.FINISHED_WORK, "Stains left", List.of(), saturday("13:30"));

        assertThatThrownBy(() -> reservation.closeDisputeWindow(saturday("14:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
