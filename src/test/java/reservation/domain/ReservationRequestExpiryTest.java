package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reservation.domain.exception.RequestExpiredException;
import reservation.domain.exception.RequestNotYetExpiredException;

/** Feature: Request expiry. */
class ReservationRequestExpiryTest {

    static Stream<Arguments> expiries() {
        return Stream.of(
                Arguments.of(thursday("09:00"), saturday("10:00"), friday("09:00")),
                Arguments.of(saturday("06:00"), saturday("10:00"), saturday("10:00")),
                Arguments.of(friday("10:00"), saturday("10:00"), saturday("10:00")));
    }

    @ParameterizedTest(name = "requested {0} for {1}: expires {2}")
    @MethodSource("expiries")
    void aRequestExpiresAfter24HoursOrAtTheBookedStartWhicheverIsSooner(Instant requested, Instant start, Instant expires) {
        Reservation reservation = aReservation().requestedAt(requested).forBooking(aBooking().startingAt(start)).requested();

        assertThat(reservation.requestExpiresAt()).isEqualTo(expires);
    }

    @Test
    void theProviderCannotAcceptAnExpiredRequest() {
        Reservation reservation = aReservation().requested();

        assertThatThrownBy(() -> reservation.accept(VERIFIED_UNTIL, friday("09:01"))).isInstanceOf(RequestExpiredException.class);
        assertThatThrownBy(() -> reservation.accept(VERIFIED_UNTIL, friday("09:00"))).isInstanceOf(RequestExpiredException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void theProviderCanAcceptUntilTheMomentItExpires() {
        Reservation reservation = aReservation().requested();

        reservation.accept(VERIFIED_UNTIL, friday("08:59:59"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void theProviderCannotDeclineAnExpiredRequest() {
        assertThatThrownBy(() -> aReservation().requested().decline(friday("09:00")))
                .isInstanceOf(RequestExpiredException.class);
    }

    @Test
    void anUnansweredRequestExpires() {
        Reservation reservation = aReservation().requested();

        reservation.expire(friday("09:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void aRequestCannotBeExpiredEarly() {
        Reservation reservation = aReservation().requested();

        assertThatThrownBy(() -> reservation.expire(friday("08:59")))
                .isInstanceOf(RequestNotYetExpiredException.class)
                .hasMessageContaining(friday("09:00").toString());
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
    }
}
