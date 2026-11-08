package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.NoShowWaitNotOverException;
import reservation.domain.exception.NotCheckedInException;
import reservation.domain.exception.ReportWindowClosedException;

/** Feature: Customer no-show. Background: reserved from Saturday 10:00, total RM230, hours fee RM180, travel RM20. */
class ReservationCustomerNoShowTest {

    @Nested
    class WhenCheckedInAtTheStart {

        @Test
        void theProviderReportsACustomerNoShowAfterTheWait() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

            Settlement settlement = reservation.reportCustomerNoShow(saturday("10:15"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(settlement.ending()).isEqualTo(Ending.CUSTOMER_NO_SHOW);
            assertThat(settlement.providerEarning()).isEqualTo(rm("74.00"));
            assertThat(settlement.serviceFee()).isEqualTo(rm("11.10"));
            assertThat(settlement.customerRefund()).isEqualTo(rm("144.90"));
            assertThat(settlement.releaseAt()).isEqualTo(saturday("11:15"));
        }

        @Test
        void aCustomerNoShowCannotBeReportedBeforeTheWaitEnds() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

            assertThatThrownBy(() -> reservation.reportCustomerNoShow(saturday("10:14")))
                    .isInstanceOf(NoShowWaitNotOverException.class);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        }
    }

    @Nested
    class WhereTheWaitStarts {

        @Test
        void theWaitStartsAtCheckInWhenTheProviderArrivesLate() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:05"));

            assertThatThrownBy(() -> reservation.reportCustomerNoShow(saturday("10:19")))
                    .isInstanceOf(NoShowWaitNotOverException.class);
            assertThat(reservation.reportCustomerNoShow(saturday("10:20")).ending()).isEqualTo(Ending.CUSTOMER_NO_SHOW);
        }

        @Test
        void theWaitStartsAtTheBookedStartWhenTheProviderArrivesEarly() {
            Reservation reservation = aReservation().checkedInAt(saturday("09:50"));

            assertThatThrownBy(() -> reservation.reportCustomerNoShow(saturday("10:14")))
                    .isInstanceOf(NoShowWaitNotOverException.class);
            assertThat(reservation.reportCustomerNoShow(saturday("10:15")).ending()).isEqualTo(Ending.CUSTOMER_NO_SHOW);
        }
    }

    @Test
    void aCustomerNoShowNeedsACheckIn() {
        assertThatThrownBy(() -> aReservation().reservedWithTeamNamed().reportCustomerNoShow(saturday("10:20")))
                .isInstanceOf(NotCheckedInException.class);
    }

    @Test
    void onlyAReservedBookingCanEndAsACustomerNoShow() {
        assertThatThrownBy(() -> aReservation().requested().reportCustomerNoShow(saturday("10:20")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Nested
    class Challenging {

        private Reservation aNoShowReportedAt1015() {
            Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
            reservation.reportCustomerNoShow(saturday("10:15"));
            return reservation;
        }

        @Test
        void theCustomerChallengesANoShowWithinTheReportWindow() {
            Reservation reservation = aNoShowReportedAt1015();

            reservation.challengeNoShow(saturday("11:00"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        }

        @Test
        void aChallengeAfterTheReportWindowIsRejected() {
            Reservation reservation = aNoShowReportedAt1015();

            assertThatThrownBy(() -> reservation.challengeNoShow(saturday("11:16")))
                    .isInstanceOf(ReportWindowClosedException.class);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        void theReportWindowClosesWhenTheEarningIsReleased() {
            Reservation reservation = aNoShowReportedAt1015();

            assertThatThrownBy(() -> reservation.challengeNoShow(saturday("11:15")))
                    .isInstanceOf(ReportWindowClosedException.class);
            reservation.challengeNoShow(saturday("11:14:59"));
            assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        }

        @Test
        void aNoShowIsChallengedOnce() {
            Reservation reservation = aNoShowReportedAt1015();
            reservation.challengeNoShow(saturday("11:00"));

            assertThatThrownBy(() -> reservation.challengeNoShow(saturday("11:05")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void onlyAReportedNoShowCanBeChallenged() {
            Reservation cancelledAfterCheckIn = aReservation().checkedInAt(saturday("09:58"));
            cancelledAfterCheckIn.cancelByCustomer(saturday("10:20"));

            assertThatThrownBy(() -> cancelledAfterCheckIn.challengeNoShow(saturday("10:30")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            assertThatThrownBy(() -> aReservation().reserved().challengeNoShow(saturday("10:30")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }
}
