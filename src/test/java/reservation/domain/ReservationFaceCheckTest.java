package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.ROSTER;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.CheckInPeriodOverException;
import reservation.domain.exception.FaceCheckAlreadyPassedException;
import reservation.domain.exception.FaceCheckAttemptsUsedException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.LocationNotCheckedException;
import reservation.domain.exception.MemberNotNamedException;
import reservation.domain.exception.NoShowWaitNotOverException;
import reservation.domain.exception.ReservationDetailRequiredException;

/**
 * Feature: Face check at check-in. Background: a reserved booking starting Saturday 10:00 with Aina and Ben
 * named, and the location check passed at 09:58.
 */
class ReservationFaceCheckTest {

    private Reservation reservation;

    @BeforeEach
    void theLocationCheckPassedAt0958() {
        reservation = aReservation().reservedWithTeamNamed();
        reservation.checkIn(120, saturday("09:58"));
    }

    @Nested
    class CompletingCheckIn {

        @Test
        void checkInCompletesWhenTheFirstPersonPasses() {
            reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("09:59"));

            assertThat(reservation.isCheckedIn()).isTrue();
            assertThat(reservation.checkedInAt()).contains(saturday("09:59"));
            assertThat(reservation.canWork("aina")).isTrue();
            assertThat(reservation.canWork("ben")).isFalse();
            assertThat(reservation.faceChecks()).containsExactly(new FaceCheck("aina", FaceCheckResult.PASSED, saturday("09:59")));
            assertThat(reservation.isFaceCheckUnavailable()).isFalse();
        }

        @Test
        void locationAloneIsNotACheckIn() {
            assertThat(reservation.isCheckedIn()).isFalse();
            assertThat(reservation.checkedInAt()).isEmpty();
            assertThat(reservation.checkInRecord()).contains(new CheckIn(saturday("09:58"), 120));
            assertThat(reservation.canWork("aina")).isFalse();
        }

        @Test
        void aFailedAttemptDoesNotCheckIn() {
            reservation.recordFaceCheck("aina", FaceCheckResult.FAILED, saturday("09:59"));

            assertThat(reservation.isCheckedIn()).isFalse();
            assertThat(reservation.canWork("aina")).isFalse();

            reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("10:01"));

            assertThat(reservation.checkedInAt()).contains(saturday("10:01"));
            assertThat(reservation.canWork("aina")).isTrue();
        }

        @Test
        void aLaterPassDoesNotMoveTheCheckInTime() {
            reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("09:59"));

            reservation.recordFaceCheck("ben", FaceCheckResult.PASSED, saturday("10:20"));

            assertThat(reservation.checkedInAt()).contains(saturday("09:59"));
            assertThat(reservation.canWork("ben")).isTrue();
            assertThat(reservation.faceChecks()).hasSize(2);
        }

        @Test
        void aNamedSupervisorTakesAFaceCheckToo() {
            Reservation withSupervisor = aReservation().reserved();
            withSupervisor.nameTeam(TeamAssignment.helpers("aina", "ben").withSupervisors("siti"), ROSTER, friday("12:00"));
            withSupervisor.checkIn(120, saturday("09:58"));

            withSupervisor.recordFaceCheck("siti", FaceCheckResult.PASSED, saturday("09:59"));

            assertThat(withSupervisor.isCheckedIn()).isTrue();
            assertThat(withSupervisor.canWork("siti")).isTrue();
        }
    }

    @Nested
    class Attempts {

        @Test
        void aPersonWhoFailsEveryAttemptCannotWork() {
            reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("09:59"));
            reservation.recordFaceCheck("ben", FaceCheckResult.FAILED, saturday("10:00"));
            reservation.recordFaceCheck("ben", FaceCheckResult.FAILED, saturday("10:01"));
            assertThat(reservation.canWork("ben")).isFalse();
            reservation.recordFaceCheck("ben", FaceCheckResult.FAILED, saturday("10:02"));

            assertThat(reservation.canWork("ben")).isFalse();
            assertThatThrownBy(() -> reservation.recordFaceCheck("ben", FaceCheckResult.PASSED, saturday("10:03")))
                    .isInstanceOf(FaceCheckAttemptsUsedException.class)
                    .hasMessageContaining("ben")
                    .hasMessageContaining("3");
            assertThat(reservation.faceChecks()).hasSize(4);
        }

        @Test
        void aPassOnTheLastAttemptLetsThemWork() {
            reservation.recordFaceCheck("ben", FaceCheckResult.FAILED, saturday("09:59"));
            reservation.recordFaceCheck("ben", FaceCheckResult.FAILED, saturday("10:00"));

            reservation.recordFaceCheck("ben", FaceCheckResult.PASSED, saturday("10:01"));

            assertThat(reservation.canWork("ben")).isTrue();
        }

        @Test
        void aPersonWhoPassedIsNotCheckedAgain() {
            reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("09:59"));

            assertThatThrownBy(() -> reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("10:00")))
                    .isInstanceOf(FaceCheckAlreadyPassedException.class)
                    .hasMessageContaining("aina");
        }

        @Test
        void onlyANamedPersonTakesAFaceCheck() {
            assertThatThrownBy(() -> reservation.recordFaceCheck("chan", FaceCheckResult.PASSED, saturday("09:59")))
                    .isInstanceOf(MemberNotNamedException.class)
                    .hasMessageContaining("chan");
            assertThat(reservation.canWork("chan")).isFalse();
        }
    }

    @Nested
    class Outage {

        @Test
        void aFaceCheckOutageNeverMakesANoShow() {
            reservation.recordFaceCheckUnavailable(saturday("10:05"));

            assertThat(reservation.isCheckedIn()).isTrue();
            assertThat(reservation.checkedInAt()).contains(saturday("10:05"));
            assertThat(reservation.isFaceCheckUnavailable()).isTrue();
            assertThat(reservation.canWork("aina")).isTrue();
            assertThat(reservation.canWork("ben")).isTrue();
            assertThat(reservation.canWork("chan")).isFalse();
        }

        @Test
        void anOutageDoesNotLetSomeoneWhoFailedEveryAttemptWork() {
            reservation.recordFaceCheck("ben", FaceCheckResult.FAILED, saturday("09:59"));
            reservation.recordFaceCheck("ben", FaceCheckResult.FAILED, saturday("10:00"));
            reservation.recordFaceCheck("ben", FaceCheckResult.FAILED, saturday("10:01"));

            reservation.recordFaceCheckUnavailable(saturday("10:05"));

            assertThat(reservation.canWork("aina")).isTrue();
            assertThat(reservation.canWork("ben")).isFalse();
        }

        @Test
        void anOutageAfterCheckInKeepsTheCheckInTime() {
            reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("09:59"));

            reservation.recordFaceCheckUnavailable(saturday("10:20"));

            assertThat(reservation.checkedInAt()).contains(saturday("09:59"));
            assertThat(reservation.canWork("ben")).isTrue();
        }
    }

    @Nested
    class TheCheckInPeriod {

        @Test
        void noFacePassByTheEndOfTheCheckInPeriodIsAProviderNoShow() {
            reservation.recordFaceCheck("aina", FaceCheckResult.FAILED, saturday("10:00"));

            CancellationResult result = reservation.markProviderNoShow(saturday("10:16"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(result.settlement().ending()).isEqualTo(Ending.PROVIDER_NO_SHOW);
        }

        @Test
        void checkInCannotCompleteAfterTheCheckInPeriod() {
            assertThatThrownBy(() -> reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("10:15:01")))
                    .isInstanceOf(CheckInPeriodOverException.class);
            assertThatThrownBy(() -> reservation.recordFaceCheckUnavailable(saturday("10:16")))
                    .isInstanceOf(CheckInPeriodOverException.class);
            assertThat(reservation.isCheckedIn()).isFalse();
            assertThat(reservation.faceChecks()).isEmpty();
        }

        @Test
        void checkInCanCompleteInTheLastSecondOfTheCheckInPeriod() {
            reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("10:15"));

            assertThat(reservation.isCheckedIn()).isTrue();
        }

        @Test
        void theCustomerNoShowWaitRunsFromTheCompletedCheckIn() {
            reservation.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("10:05"));

            assertThatThrownBy(() -> reservation.reportCustomerNoShow(saturday("10:19")))
                    .isInstanceOf(NoShowWaitNotOverException.class);
            reservation.reportCustomerNoShow(saturday("10:20"));
            assertThat(reservation.ending()).contains(Ending.CUSTOMER_NO_SHOW);
        }

        @Test
        void aCancellationAfterOnlyTheLocationCheckIsNotACustomerNoShow() {
            Reservation early = aReservation().reservedWithTeamNamed();
            early.checkIn(120, saturday("09:50"));

            Settlement settlement = early.cancelByCustomer(saturday("09:55"));

            assertThat(settlement.ending()).isNotEqualTo(Ending.CUSTOMER_CANCELLED_AFTER_CHECK_IN);
        }
    }

    @Nested
    class Order {

        @Test
        void faceChecksComeAfterTheLocationCheck() {
            Reservation located = aReservation().reservedWithTeamNamed();

            assertThatThrownBy(() -> located.recordFaceCheck("aina", FaceCheckResult.PASSED, saturday("09:58")))
                    .isInstanceOf(LocationNotCheckedException.class);
            assertThatThrownBy(() -> located.recordFaceCheckUnavailable(saturday("09:58")))
                    .isInstanceOf(LocationNotCheckedException.class);
            assertThat(located.canWork("aina")).isFalse();
        }

        @Test
        void onlyAReservedBookingTakesFaceChecks() {
            assertThatThrownBy(() -> aReservation().requested().recordFaceCheck("aina", FaceCheckResult.PASSED, thursday("10:00")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void nobodyCanWorkBeforeTheTeamIsNamed() {
            assertThat(aReservation().reserved().canWork("aina")).isFalse();
        }

        @Test
        void aFaceCheckHasAPersonAResultAndATime() {
            assertThatThrownBy(() -> new FaceCheck(" ", FaceCheckResult.PASSED, saturday("10:00")))
                    .isInstanceOf(ReservationDetailRequiredException.class);
            assertThatThrownBy(() -> new FaceCheck("aina", null, saturday("10:00"))).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new FaceCheck("aina", FaceCheckResult.PASSED, null)).isInstanceOf(NullPointerException.class);
        }
    }
}
