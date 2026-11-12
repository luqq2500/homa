package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.anOffering;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.InstantBookCannotBeDeclinedException;
import reservation.domain.exception.NotInstantBookException;
import reservation.domain.exception.PaymentNotSecuredException;
import reservation.domain.exception.RequestExpiredException;

/** Feature: Instant book. Background: requested Thursday 09:00 for Saturday 10:00. */
class ReservationInstantBookTest {

    private static ReservationBuilders.ReservationBuilder toAnInstantBookProvider() {
        return aReservation().withOffering(anOffering().withInstantBook());
    }

    @Test
    void anInstantBookRequestIsAcceptedOnceItIsPaid() {
        Reservation reservation = toAnInstantBookProvider().requested();

        reservation.acceptInstantly(VERIFIED_UNTIL, thursday("09:01"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.offering().acceptancePolicy()).isEqualTo(AcceptancePolicy.INSTANT_BOOK);
    }

    @Test
    void anInstantBookRequestIsNotAcceptedBeforeItIsPaid() {
        Reservation reservation = toAnInstantBookProvider().requestedUnpaid();

        assertThatThrownBy(() -> reservation.acceptInstantly(VERIFIED_UNTIL, thursday("09:01")))
                .isInstanceOf(PaymentNotSecuredException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void anExpiredInstantBookRequestIsNotAccepted() {
        Reservation reservation = toAnInstantBookProvider().requested();

        assertThatThrownBy(() -> reservation.acceptInstantly(VERIFIED_UNTIL, friday("09:00")))
                .isInstanceOf(RequestExpiredException.class);
    }

    @Test
    void onlyAnInstantBookRequestIsAcceptedInstantly() {
        Reservation reservation = aReservation().requested();

        assertThatThrownBy(() -> reservation.acceptInstantly(VERIFIED_UNTIL, thursday("09:01")))
                .isInstanceOf(NotInstantBookException.class)
                .hasMessageContaining(reservation.id());
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void theProviderCannotDeclineAnInstantBookRequest() {
        Reservation reservation = toAnInstantBookProvider().requestedUnpaid();

        assertThatThrownBy(() -> reservation.decline(thursday("09:01")))
                .isInstanceOf(InstantBookCannotBeDeclinedException.class)
                .hasMessageContaining(reservation.id());
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void anInstantBookRequestWhoseTimeIsTakenStillEnds() {
        Reservation reservation = toAnInstantBookProvider().requested();

        reservation.declineForTakenTime(thursday("09:01"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(reservation.ending()).contains(Ending.TIME_TAKEN);
    }
}
