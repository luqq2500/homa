package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.PaymentNotSecuredException;

/** Feature: A request is paid before the provider answers. The total is RM230.00. */
class ReservationPaymentConfirmationTest {

    @Test
    void aNewRequestWaitsForItsPayment() {
        Reservation reservation = aReservation().requestedUnpaid();

        assertThat(reservation.isPaymentSecured()).isFalse();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void theProviderCannotAcceptARequestBeforeItsPaymentIsSecured() {
        Reservation reservation = aReservation().requestedUnpaid();

        assertThatThrownBy(() -> reservation.accept(VERIFIED_UNTIL, thursday("10:00")))
                .isInstanceOf(PaymentNotSecuredException.class)
                .hasMessage("Reservation " + reservation.id() + " cannot be accepted before its payment is secured");
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void theProviderAcceptsOnceThePaymentIsSecured() {
        Reservation reservation = aReservation().requestedUnpaid();

        reservation.confirmPayment(rm("230.00"));
        reservation.accept(VERIFIED_UNTIL, thursday("10:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void aPaymentBelowTheTotalIsNotEnough() {
        Reservation reservation = aReservation().requestedUnpaid();

        reservation.confirmPayment(rm("229.99"));

        assertThat(reservation.isPaymentSecured()).isFalse();
        assertThatThrownBy(() -> reservation.accept(VERIFIED_UNTIL, thursday("10:00")))
                .isInstanceOf(PaymentNotSecuredException.class);
    }

    @Test
    void aPaymentOfExactlyTheTotalIsEnough() {
        Reservation reservation = aReservation().requestedUnpaid();

        reservation.confirmPayment(rm("230.00"));

        assertThat(reservation.isPaymentSecured()).isTrue();
    }

    @Test
    void anEditThatRaisesTheTotalWaitsForPaymentAgain() {
        Reservation reservation = aReservation().requested();

        reservation.edit(aBooking().forHours(4).build(), VERIFIED_UNTIL, thursday("10:00"));

        assertThat(reservation.price().total()).isEqualTo(rm("299.00"));
        assertThat(reservation.isPaymentSecured()).isFalse();
        assertThatThrownBy(() -> reservation.accept(VERIFIED_UNTIL, thursday("10:30")))
                .isInstanceOf(PaymentNotSecuredException.class);
    }

    @Test
    void theProviderAcceptsAnEditedRequestOnceTheNewTotalIsSecured() {
        Reservation reservation = aReservation().requested();
        reservation.edit(aBooking().forHours(4).build(), VERIFIED_UNTIL, thursday("10:00"));

        reservation.confirmPayment(rm("299.00"));
        reservation.accept(VERIFIED_UNTIL, thursday("10:30"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void anEditThatLowersTheTotalStaysPaid() {
        Reservation reservation = aReservation().requested();

        reservation.edit(aBooking().withHelpers(1).build(), VERIFIED_UNTIL, thursday("10:00"));

        assertThat(reservation.isPaymentSecured()).isTrue();
    }

    @Test
    void onlyARequestIsConfirmedAsPaid() {
        Reservation reserved = aReservation().reserved();

        assertThatThrownBy(() -> reserved.confirmPayment(rm("230.00")))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("confirmed as paid");
    }

    @Test
    void aRequestThatEndedIsNotConfirmedAsPaid() {
        Reservation reservation = aReservation().requestedUnpaid();
        reservation.paymentFailed();

        assertThatThrownBy(() -> reservation.confirmPayment(rm("230.00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(reservation.isPaymentSecured()).isFalse();
    }

    @Test
    void aConfirmationNeedsAnAmount() {
        Reservation reservation = aReservation().requestedUnpaid();

        assertThatThrownBy(() -> reservation.confirmPayment(null)).isInstanceOf(NullPointerException.class);
        assertThat(reservation.isPaymentSecured()).isFalse();
    }
}
