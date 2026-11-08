package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reservation.domain.exception.AlreadyCheckedInException;
import reservation.domain.exception.CheckInPeriodNotOverException;
import reservation.domain.exception.CheckInPeriodOverException;
import reservation.domain.exception.CheckInTooEarlyException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.TeamNotNamedException;
import reservation.domain.exception.TooFarToCheckInException;

/** Feature: Check-in. Background: reserved from Saturday 10:00 with the team named. */
class ReservationCheckInTest {

    @Nested
    class CheckingIn {

        static Stream<Arguments> checkIns() {
            return Stream.of(
                    Arguments.of(saturday("09:45"), 120, null),
                    Arguments.of(saturday("09:44"), 120, CheckInTooEarlyException.class),
                    Arguments.of(saturday("10:05"), 120, null),
                    Arguments.of(saturday("10:15"), 300, null),
                    Arguments.of(saturday("10:05"), 301, TooFarToCheckInException.class),
                    Arguments.of(saturday("10:16"), 50, CheckInPeriodOverException.class),
                    Arguments.of(saturday("10:15:01"), 50, CheckInPeriodOverException.class));
        }

        @ParameterizedTest(name = "checking in at {0} from {1} metres: {2}")
        @MethodSource("checkIns")
        void checkInNeedsTheDeviceNearTheCustomerWithinTheCheckInPeriod(
                Instant at, int metres, Class<? extends Throwable> rejection) {
            Reservation reservation = aReservation().reservedWithTeamNamed();

            if (rejection == null) {
                reservation.checkIn(metres, at);
                assertThat(reservation.checkInRecord()).contains(new CheckIn(at, metres));
            } else {
                assertThatThrownBy(() -> reservation.checkIn(metres, at)).isInstanceOf(rejection);
                assertThat(reservation.checkInRecord()).isEmpty();
            }
        }

        @Test
        void checkInIsLockedUntilTheTeamIsNamed() {
            assertThatThrownBy(() -> aReservation().reserved().checkIn(50, saturday("10:05")))
                    .isInstanceOf(TeamNotNamedException.class);
        }

        @Test
        void theProviderChecksInOnce() {
            Reservation reservation = aReservation().checkedInAt(saturday("09:58"));

            assertThatThrownBy(() -> reservation.checkIn(50, saturday("10:00"))).isInstanceOf(AlreadyCheckedInException.class);
        }

        @Test
        void onlyAReservedBookingIsCheckedInto() {
            assertThatThrownBy(() -> aReservation().requested().checkIn(50, thursday("10:00")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Nested
    class ProviderNoShow {

        @Test
        void noCheckInByTheEndOfTheCheckInPeriodIsAProviderNoShow() {
            Reservation reservation = aReservation().reservedWithTeamNamed();

            CancellationResult result = reservation.markProviderNoShow(saturday("10:16"));

            assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(result.settlement().ending()).isEqualTo(Ending.PROVIDER_NO_SHOW);
            assertThat(result.settlement().customerRefund()).isEqualTo(rm("230.00"));
            assertThat(result.fineAmount()).isEqualTo(rm("54.00"));
            assertThat(result.fine().orElseThrow().reason()).isEqualTo(FineReason.NO_SHOW);
        }

        @Test
        void providerNoShowIsMarkedOnlyAfterCheckInPeriodEnds() {
            Reservation reservation = aReservation().reservedWithTeamNamed();

            assertThatThrownBy(() -> reservation.markProviderNoShow(saturday("10:15")))
                    .isInstanceOf(CheckInPeriodNotOverException.class);
            assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        }

        @Test
        void aProviderWhoCheckedInIsNotANoShow() {
            assertThatThrownBy(() -> aReservation().checkedInAt(saturday("10:00")).markProviderNoShow(saturday("10:16")))
                    .isInstanceOf(AlreadyCheckedInException.class);
        }

        @Test
        void aRequestIsNeverAProviderNoShow() {
            assertThatThrownBy(() -> aReservation().requested().markProviderNoShow(saturday("10:16")))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }
}
