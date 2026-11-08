package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.HoursOutsideProviderRangeException;
import reservation.domain.exception.RequestExpiredException;
import reservation.domain.exception.ReservationNotEditableException;

/** Feature: Editing a request. */
class ReservationEditingTest {

    @Test
    void theCustomerEditsARequestBeforeItIsAccepted() {
        Reservation reservation = aReservation().forBooking(aBooking().forHours(3)).requested();
        assertThat(reservation.price().total()).isEqualTo(rm("230.00"));

        PriceChange change = reservation.edit(aBooking().forHours(4).build(), VERIFIED_UNTIL, thursday("10:00"));

        assertThat(reservation.price().total()).isEqualTo(rm("299.00"));
        assertThat(change.previousTotal()).isEqualTo(rm("230.00"));
        assertThat(change.newTotal()).isEqualTo(rm("299.00"));
        assertThat(change.difference()).isEqualTo(rm("69.00"));
        assertThat(reservation.details().hours()).isEqualTo(4);
    }

    @Test
    void choosingFewerHelpersLowersTheTotal() {
        Reservation reservation = aReservation().requested();

        PriceChange change = reservation.edit(aBooking().withHelpers(1).build(), VERIFIED_UNTIL, thursday("10:00"));

        assertThat(change.difference()).isEqualTo(rm("-103.50"));
    }

    @Test
    void aReservedBookingCannotBeEdited() {
        Reservation reservation = aReservation().reserved();

        assertThatThrownBy(() -> reservation.edit(aBooking().forHours(4).build(), VERIFIED_UNTIL, thursday("12:00")))
                .isInstanceOf(ReservationNotEditableException.class)
                .hasMessageContaining("RESERVED");
    }

    @Test
    void anExpiredRequestCannotBeEdited() {
        assertThatThrownBy(() -> aReservation().requested().edit(aBooking().forHours(4).build(), VERIFIED_UNTIL, friday("09:00")))
                .isInstanceOf(RequestExpiredException.class);
    }

    @Test
    void anEditMustStillFitTheProvidersRules() {
        Reservation reservation = aReservation().requested();

        assertThatThrownBy(() -> reservation.edit(aBooking().forHours(5).build(), VERIFIED_UNTIL, thursday("10:00")))
                .isInstanceOf(HoursOutsideProviderRangeException.class);
        assertThat(reservation.details().hours()).isEqualTo(3);
        assertThat(reservation.price().total()).isEqualTo(rm("230.00"));
    }

    @Test
    void editingDoesNotResetTheExpiry() {
        Reservation reservation = aReservation().requested();

        reservation.edit(aBooking().forHours(4).build(), VERIFIED_UNTIL, thursday("20:00"));

        assertThat(reservation.requestExpiresAt()).isEqualTo(friday("09:00"));
    }

    @Test
    void movingTheStartEarlierCanBringTheExpiryForward() {
        Reservation reservation = aReservation().requested();

        reservation.edit(aBooking().startingAt(friday("08:00")).build(), VERIFIED_UNTIL, thursday("10:00"));

        assertThat(reservation.requestExpiresAt()).isEqualTo(friday("08:00"));
    }
}
