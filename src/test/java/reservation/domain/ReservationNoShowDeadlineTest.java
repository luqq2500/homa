package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.saturday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.NoShowReportTooLateException;

/** Feature: The customer no-show deadline. Background: Saturday 10:00 to 13:00, checked in at 10:00. */
class ReservationNoShowDeadlineTest {

    @Test
    void aCustomerNoShowCanBeReportedAtTheBookedEnd() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

        reservation.reportCustomerNoShow(saturday("13:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.ending()).contains(Ending.CUSTOMER_NO_SHOW);
    }

    @Test
    void aCustomerNoShowCannotBeReportedAfterTheBookedEnd() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

        assertThatThrownBy(() -> reservation.reportCustomerNoShow(saturday("13:01")))
                .isInstanceOf(NoShowReportTooLateException.class)
                .hasMessageContaining("booked end");
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }
}
