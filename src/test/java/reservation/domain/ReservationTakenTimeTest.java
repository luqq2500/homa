package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.RequestExpiredException;

/** Feature: Requests declined when the time is taken. Background: requested Thursday 09:00, expiring Friday 09:00. */
class ReservationTakenTimeTest {

    @Test
    void anOpenRequestIsDeclinedWhenAnotherBookingTakesItsTime() {
        Reservation reservation = aReservation().requested();

        reservation.declineForTakenTime(thursday("15:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(reservation.ending()).contains(Ending.TIME_TAKEN);
    }

    @Test
    void onlyARequestCanBeDeclinedForTakenTime() {
        Reservation reservation = aReservation().reserved();

        assertThatThrownBy(() -> reservation.declineForTakenTime(thursday("15:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(reservation.ending()).isEmpty();
    }

    @Test
    void anExpiredRequestIsNotDeclinedForTakenTime() {
        Reservation reservation = aReservation().requested();

        assertThatThrownBy(() -> reservation.declineForTakenTime(friday("09:00")))
                .isInstanceOf(RequestExpiredException.class);
    }

    @Test
    void anOrdinaryDeclineHasNoEnding() {
        Reservation reservation = aReservation().requested();

        reservation.decline(thursday("15:00"));

        assertThat(reservation.ending()).isEmpty();
    }
}
