package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.anAddressIn;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.AddressNotEditableException;

/** Feature: The address can't be edited on a request. */
class ReservationAddressTest {

    @Test
    void editingARequestToAnotherAddressIsRefused() {
        Reservation reservation = aReservation().requested();
        Address elsewhere = Address.of("8 Jalan Bunga", City.of("Cyberjaya"));

        assertThatThrownBy(() -> reservation.edit(aBooking().build().forBookingAgain(aBooking().build().start(), elsewhere), VERIFIED_UNTIL, thursday("10:00")))
                .isInstanceOf(AddressNotEditableException.class)
                .hasMessageContaining("12 Jalan Teknokrat 3");
        assertThat(reservation.details().address()).isEqualTo(anAddressIn("Cyberjaya"));
    }

    @Test
    void editingTheOtherDetailsKeepsTheAddress() {
        Reservation reservation = aReservation().requested();

        reservation.edit(aBooking().forHours(4).build(), VERIFIED_UNTIL, thursday("10:00"));

        assertThat(reservation.details().hours()).isEqualTo(4);
        assertThat(reservation.details().address()).isEqualTo(anAddressIn("Cyberjaya"));
    }
}
