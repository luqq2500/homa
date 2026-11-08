package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static reservation.domain.ReservationBuilders.VERIFIED_UNTIL;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.anOffering;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;

/** Feature: Pricing. */
class ReservationPricingTest {

    @Test
    void aSeparateTravelFeeIsAddedToTheHoursFeeBeforeTheServiceFee() {
        PriceBreakdown price = aReservation()
                .withOffering(anOffering().charging("30").withSeparateTravelFee("20"))
                .forBooking(aBooking().withHelpers(2).forHours(3))
                .requested()
                .price();

        assertThat(price.hoursFee()).isEqualTo(rm("180.00"));
        assertThat(price.travelFee()).isEqualTo(rm("20.00"));
        assertThat(price.providerPrice()).isEqualTo(rm("200.00"));
        assertThat(price.serviceFee()).isEqualTo(rm("30.00"));
        assertThat(price.total()).isEqualTo(rm("230.00"));
    }

    @Test
    void withTravelIncludedTheProviderPriceIsTheHoursFee() {
        PriceBreakdown price = aReservation()
                .withOffering(anOffering().charging("35").withTravelIncluded())
                .forBooking(aBooking().withHelpers(2).forHours(3))
                .requested()
                .price();

        assertThat(price.hoursFee()).isEqualTo(rm("210.00"));
        assertThat(price.travelFee()).isEqualTo(rm("0.00"));
        assertThat(price.providerPrice()).isEqualTo(rm("210.00"));
        assertThat(price.serviceFee()).isEqualTo(rm("31.50"));
        assertThat(price.total()).isEqualTo(rm("241.50"));
    }

    @Test
    void choosingFewerJobsDoesNotChangeThePrice() {
        Reservation reservation = aReservation().forBooking(aBooking().forJobs("KITCHEN", "LAUNDRY_FOLD")).requested();
        assertThat(reservation.price().total()).isEqualTo(rm("230.00"));

        reservation.edit(aBooking().forJobs("KITCHEN").build(), VERIFIED_UNTIL, thursday("10:00"));

        assertThat(reservation.price().total()).isEqualTo(rm("230.00"));
    }
}
