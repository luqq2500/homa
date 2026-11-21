package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.TAPPED_PAY_AT;
import static com.homa.reservation.domain.ReservationFixtures.aisyahAddress;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.replica.CustomerStanding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CheckoutTest {

    private final Marketplace market = Marketplace.withKemasHome();

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah taps Pay at 19:58 and pays RM200 for Saturday 10:00–14:00. Nothing is held for her while she pays.")
    void checkoutCarriesTheBookingAndThePriceAndHoldsNothing() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);

        assertThat(checkout.id()).isNotNull();
        assertThat(checkout.booking()).isEqualTo(standardBooking());
        assertThat(checkout.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(market.providerSchedule().blocks()).isEmpty();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).blocks()).isEmpty();
    }

    @Test
    @Tag("RES-2")
    @DisplayName("Aisyah books at her saved home address, whatever address the booking was drafted with.")
    void checkoutIsAtTheCustomersHomeAddress() {
        Booking drafted = standardBooking().at(ReservationFixtures.anotherCustomerAddress());

        assertThat(market.checkout(drafted, TAPPED_PAY_AT).booking().address()).isEqualTo(aisyahAddress());
    }

    @Test
    @Tag("RES-1")
    @DisplayName("A booking the provider doesn't admit can't start a checkout.")
    void checkoutIsRefusedWhenTheProviderDoesNotAdmitTheBooking() {
        market.providerBecomes(kemasHomeProvider().cities("CYBERJAYA").build());

        assertRefused(() -> market.checkout(standardBooking(), TAPPED_PAY_AT), BookingRefusal.CITY_NOT_COVERED);
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
