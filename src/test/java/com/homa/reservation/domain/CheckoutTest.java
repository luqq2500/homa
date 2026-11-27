package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.TAPPED_PAY_AT;
import static com.homa.reservation.domain.ReservationFixtures.aisyahAddress;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.replica.CustomerStanding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CheckoutTest {

    private static final String AISYAHS_HUSBAND_ID = "customer-aisyahs-husband";

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
    @Tag("RES-11")
    @DisplayName("Persistence: a stored checkout comes back from its id, booking and price, and its confirmed payment makes the reservation.")
    void aStoredCheckoutComesBackFromItsParts() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);

        Checkout stored = new Checkout(checkout.id(), checkout.booking(), checkout.quote());

        assertThat(stored).isEqualTo(checkout);
        assertThat(market.pay(stored, REQUESTED_AT).checkoutId())
                .isEqualTo(checkout.id());
        assertThatThrownBy(() -> new Checkout(checkout.id(), checkout.booking(), null))
                .isInstanceOf(NullPointerException.class);
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
    @Tag("CUS-3")
    @DisplayName("Aisyah's husband registers and asks Kemas Home about a ladder. When he taps Reserve, he's asked to verify first.")
    void unverifiedCustomerIsAskedToVerifyBeforePaying() {
        CustomerStanding husband = CustomerStanding.registered(AISYAHS_HUSBAND_ID, aisyahAddress());
        market.register(husband);
        Booking booking = new Booking(AISYAHS_HUSBAND_ID, standardBooking().providerId(), standardBooking().jobs(),
                standardBooking().jobTime(), standardBooking().helpers(), aisyahAddress());

        assertThat(husband.canReserve()).isFalse();
        assertThatThrownBy(() -> market.checkout(booking, TAPPED_PAY_AT))
                .isInstanceOfSatisfying(BookingRefusedException.class, refused -> {
                    assertThat(refused.refusal()).isEqualTo(BookingRefusal.CUSTOMER_NOT_VERIFIED);
                    assertThat(refused.refusal().asksForVerification()).isTrue();
                });
        assertThat(market.providerSchedule().blocks()).isEmpty();
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("Once he verifies, he can reserve.")
    void verifiedCustomerCanReserve() {
        CustomerStanding husband = CustomerStanding.registered(AISYAHS_HUSBAND_ID, aisyahAddress()).verified();

        assertThat(husband.identityVerified()).isTrue();
        assertThat(husband.canReserve()).isTrue();
        assertThat(husband.refusal()).isEmpty();
        assertThat(husband.suspended()).isFalse();
        assertThat(husband.homeAddress()).isEqualTo(aisyahAddress());
        assertThat(ReservationFixtures.aisyah().canReserve()).isTrue();
    }

    @Test
    @Tag("CUS-5")
    @DisplayName("A suspended customer can't reserve.")
    void suspendedCustomerCannotStartACheckout() {
        market.register(new CustomerStanding(AISYAH_CUSTOMER_ID, true, true, aisyahAddress()));

        assertRefused(() -> market.checkout(standardBooking(), TAPPED_PAY_AT), BookingRefusal.CUSTOMER_SUSPENDED);
        assertThat(BookingRefusal.CUSTOMER_SUSPENDED.asksForVerification()).isFalse();
    }

    @Test
    @Tag("CUS-5")
    @DisplayName("A suspended customer who isn't verified is told of the suspension, because verifying wouldn't let them reserve.")
    void suspensionIsNamedBeforeVerification() {
        CustomerStanding suspendedUnverified = new CustomerStanding(AISYAHS_HUSBAND_ID, false, true, aisyahAddress());

        assertThat(suspendedUnverified.refusal()).contains(BookingRefusal.CUSTOMER_SUSPENDED);
        assertThat(suspendedUnverified.verified().refusal()).contains(BookingRefusal.CUSTOMER_SUSPENDED);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Her new address on Thursday applies only to new bookings: she stays verified, and a suspension stays.")
    void customerWhoMovesKeepsTheirStandingAndBooksAtTheNewAddress() {
        ServiceAddress cyberjaya = new ServiceAddress("7, Persiaran Cyberjaya", "CYBERJAYA", "Lobby");
        CustomerStanding aisyah = CustomerStanding.registered(AISYAH_CUSTOMER_ID, aisyahAddress()).verified();
        CustomerStanding suspended = new CustomerStanding(AISYAHS_HUSBAND_ID, false, true, aisyahAddress());

        assertThat(aisyah.movedTo(cyberjaya))
                .isEqualTo(new CustomerStanding(AISYAH_CUSTOMER_ID, true, false, cyberjaya));
        assertThat(suspended.movedTo(cyberjaya))
                .isEqualTo(new CustomerStanding(AISYAHS_HUSBAND_ID, false, true, cyberjaya));
    }
}
