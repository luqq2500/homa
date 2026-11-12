package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.anOffering;
import static reservation.domain.ReservationBuilders.theLaunchPolicyWithHours;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.HoursOutsideHomaRangeException;

/** Feature: Homa narrows its duration range. A request must fit Homa's range as well as the provider's hours. */
class ReservationHomaRangeTest {

    private static ReservationBuilders.ReservationBuilder aProviderWithHours1To6UnderHomas(int min, int max) {
        return aReservation().withOffering(anOffering().withHours(1, 6)).underPolicy(theLaunchPolicyWithHours(min, max));
    }

    @Test
    void aRequestMustFitHomasRangeToo() {
        assertThatThrownBy(() -> aProviderWithHours1To6UnderHomas(1, 4).forBooking(aBooking().forHours(5)).requested())
                .isInstanceOf(HoursOutsideHomaRangeException.class)
                .hasMessageContaining("5 hours")
                .hasMessageContaining("1 to 4");
    }

    @Test
    void theEndsOfHomasRangeCanBeBooked() {
        assertThat(aProviderWithHours1To6UnderHomas(2, 4).forBooking(aBooking().forHours(4)).requested().status())
                .isEqualTo(ReservationStatus.REQUESTED);
        assertThat(aProviderWithHours1To6UnderHomas(2, 4).forBooking(aBooking().forHours(2)).requested().status())
                .isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    void aRequestBelowHomasRangeIsRefused() {
        assertThatThrownBy(() -> aProviderWithHours1To6UnderHomas(2, 4).forBooking(aBooking().forHours(1)).requested())
                .isInstanceOf(HoursOutsideHomaRangeException.class);
    }

    @Test
    void anEditMustFitHomasRangeToo() {
        Reservation reservation = aProviderWithHours1To6UnderHomas(1, 4).requested();

        assertThatThrownBy(() -> reservation.edit(aBooking().forHours(5).build(), VERIFIED_UNTIL, thursday("10:00")))
                .isInstanceOf(HoursOutsideHomaRangeException.class);
        assertThat(reservation.details().hours()).isEqualTo(3);
    }
}
