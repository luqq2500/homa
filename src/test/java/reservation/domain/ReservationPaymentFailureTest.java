package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.InvalidStatusTransitionException;

/** Feature: A payment that fails at request. The request ends, so the provider never answers an unpaid one. */
class ReservationPaymentFailureTest {

    @Test
    void aFailedPaymentEndsTheRequest() {
        Reservation reservation = aReservation().requested();

        reservation.paymentFailed();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.ending()).contains(Ending.PAYMENT_FAILED);
    }

    @Test
    void theProviderCannotAcceptARequestWhosePaymentFailed() {
        Reservation reservation = aReservation().requested();
        reservation.paymentFailed();

        assertThatThrownBy(() -> reservation.accept(VERIFIED_UNTIL, thursday("12:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void theCustomerCannotCancelARequestWhosePaymentFailed() {
        Reservation reservation = aReservation().requested();
        reservation.paymentFailed();

        assertThatThrownBy(() -> reservation.cancelByCustomer(thursday("12:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(reservation.ending()).contains(Ending.PAYMENT_FAILED);
    }

    @Test
    void onlyARequestEndsForAFailedPayment() {
        Reservation reserved = aReservation().reserved();

        assertThatThrownBy(reserved::paymentFailed)
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("ended for a failed payment");
        assertThat(reserved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reserved.ending()).isEmpty();
    }

    @Test
    void aRequestEndsForAFailedPaymentOnlyOnce() {
        Reservation reservation = aReservation().requested();
        reservation.paymentFailed();

        assertThatThrownBy(reservation::paymentFailed).isInstanceOf(InvalidStatusTransitionException.class);
    }
}
