package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_TIME;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.TAPPED_PAY_AT;
import static com.homa.reservation.domain.ReservationFixtures.aisyahAddress;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import com.homa.reservation.domain.terms.CancellationTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CheckoutTest {

    private final Marketplace market = Marketplace.withKemasHome();

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah taps Pay at 19:58: she pays RM200 under Kemas Home's terms while the provider's 9:45–14:30 and her own 10:00–14:00 are held until 20:13.")
    void checkoutCarriesTheTermsThePriceAndTheHeldTime() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);

        assertThat(checkout.booking()).isEqualTo(standardBooking());
        assertThat(checkout.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(checkout.terms()).isEqualTo(market.provider().termsFor(aisyahAddress()));
        assertThat(checkout.terms().acceptancePolicy()).isEqualTo(AcceptancePolicy.REQUEST);
        assertThat(checkout.terms().cancellationTier()).isEqualTo(CancellationTier.STANDARD);
        assertThat(checkout.providerBlock()).isEqualTo(new TimeInterval(saturday(9, 45), saturday(14, 30)));
        assertThat(checkout.customerBlock()).isEqualTo(JOB_TIME);
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).isHolding(checkout.id(), TAPPED_PAY_AT)).isTrue();
    }

    @Test
    @Tag("RES-1")
    @DisplayName("A booking the provider doesn't admit can't start a checkout, and nothing is held.")
    void checkoutIsRefusedWhenTheProviderDoesNotAdmitTheBooking() {
        market.providerBecomes(kemasHomeProvider().cities("CYBERJAYA").build());

        assertRefused(() -> market.checkout(standardBooking(), TAPPED_PAY_AT), BookingRefusal.CITY_NOT_COVERED);
        assertThat(market.providerSchedule().isFree(JOB_TIME, TAPPED_PAY_AT)).isTrue();
    }

    @Test
    @Tag("CUS-5")
    @DisplayName("A suspended customer can't make reservations.")
    void suspendedCustomerCannotStartACheckout() {
        market.register(new CustomerStanding(AISYAH_CUSTOMER_ID, true, aisyahAddress()));

        assertRefused(() -> market.checkout(standardBooking(), TAPPED_PAY_AT), BookingRefusal.CUSTOMER_CANNOT_RESERVE);
        assertThat(ReservationFixtures.aisyah().canReserve()).isTrue();
    }
}
