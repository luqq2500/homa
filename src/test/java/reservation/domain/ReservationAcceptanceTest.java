package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.InvalidStatusTransitionException;

/** Feature: Accepting and declining. */
class ReservationAcceptanceTest {

    @Test
    void theProviderAcceptsARequest() {
        Reservation reservation = aReservation().requested();

        reservation.accept(VERIFIED_UNTIL, thursday("12:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void theProviderDeclinesARequest() {
        Reservation reservation = aReservation().requested();

        reservation.decline(thursday("12:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DECLINED);
    }

    @Test
    void onlyARequestedReservationCanBeAccepted() {
        Reservation reservation = aReservation().requested();
        reservation.decline(thursday("12:00"));

        assertThatThrownBy(() -> reservation.accept(VERIFIED_UNTIL, thursday("13:00")))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("A DECLINED reservation cannot be accepted");
    }

    @Test
    void onlyARequestedReservationCanBeDeclinedOrExpired() {
        Reservation reservation = aReservation().reserved();

        assertThatThrownBy(() -> reservation.decline(thursday("13:00"))).isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> reservation.expire(friday("09:00"))).isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }
}
