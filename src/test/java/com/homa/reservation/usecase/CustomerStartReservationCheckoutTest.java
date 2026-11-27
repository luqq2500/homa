package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;
import static com.homa.reservation.usecase.ReservationTestBed.KITCHEN_CLEANING;
import static com.homa.reservation.usecase.ReservationTestBed.LAUNDRY_FOLDING;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.SHAH_ALAM_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.TAPPED_PAY_AT;
import static com.homa.reservation.usecase.ReservationTestBed.UNVERIFIED_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.standardCheckout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ReservationCheckoutStartedIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.Checkout;
import com.homa.reservation.domain.JobSelection;
import com.homa.reservation.domain.JobTime;
import com.homa.reservation.domain.ServiceAddress;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.usecase.dto.StartReservationCheckoutRequest;
import com.homa.reservation.usecase.dto.StartReservationCheckoutResponse;
import com.homa.reservation.usecase.exception.CustomerNotFoundException;
import com.homa.reservation.usecase.exception.ProviderNotFoundException;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerStartReservationCheckoutTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private StartReservationCheckoutResponse start(String customerId) {
        return bed.startCheckout.start(standardCheckout(customerId, KEMAS_HOME_PROVIDER_ID));
    }

    private void assertRefused(ThrowingCallable call, BookingRefusal refusal) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BookingRefusedException.class,
                refused -> assertThat(refused.refusal()).isEqualTo(refusal));
        assertThat(bed.publisher.published()).isEmpty();
        assertThat(bed.checkouts.all()).isEmpty();
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah taps Pay at 19:58: the checkout carries the booking and RM200, and nothing is held.")
    void checkoutOpensThePaymentAndHoldsNothing() {
        StartReservationCheckoutResponse started = start(AISYAH_CUSTOMER_ID);

        Checkout checkout = bed.checkouts.all().get(0);
        assertThat(started).isEqualTo(new StartReservationCheckoutResponse("STARTED", checkout.id().value(),
                Money.myr(200)));
        assertThat(checkout.booking().customerId()).isEqualTo(AISYAH_CUSTOMER_ID);
        assertThat(checkout.booking().providerId()).isEqualTo(KEMAS_HOME_PROVIDER_ID);
        assertThat(checkout.booking().jobs()).isEqualTo(JobSelection.of(KITCHEN_CLEANING, LAUNDRY_FOLDING));
        assertThat(checkout.booking().jobTime()).isEqualTo(JobTime.of(JOB_START, STANDARD_HOURS));
        assertThat(checkout.booking().helpers()).isEqualTo(2);
        assertThat(checkout.quote().total()).isEqualTo(Money.myr(200));
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationCheckoutStartedIE.class)).isEqualTo(
                new ReservationCheckoutStartedIE("reservation-event-1", checkout.id().value(), AISYAH_CUSTOMER_ID,
                        KEMAS_HOME_PROVIDER_ID, Money.myr(200), TAPPED_PAY_AT));
    }

    @Test
    @Tag("RES-2")
    @DisplayName("Aisyah picks kitchen cleaning and laundry folding, Saturday 10:00, 4 hours, 2 helpers and her saved address.")
    void checkoutIsAtTheCustomersHomeAddress() {
        start(AISYAH_CUSTOMER_ID);

        assertThat(bed.checkouts.all().get(0).booking().address())
                .isEqualTo(bed.customers.findById(AISYAH_CUSTOMER_ID).orElseThrow().homeAddress());
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("Aisyah's husband registered but isn't verified: he is asked to verify first, and no payment opens.")
    void unverifiedCustomerIsAskedToVerifyAndNothingStarts() {
        assertThat(start(UNVERIFIED_CUSTOMER_ID))
                .isEqualTo(new StartReservationCheckoutResponse("VERIFICATION_REQUIRED", null, null));
        assertThat(bed.checkouts.all()).isEmpty();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("CUS-5")
    @DisplayName("A suspended customer can't reserve, and isn't asked to verify instead.")
    void suspendedCustomerIsRefused() {
        bed.customers.save(new CustomerStanding(MEI_CUSTOMER_ID, true, true,
                new ServiceAddress("3A, Presint 9", "PUTRAJAYA", "")));

        assertRefused(() -> start(MEI_CUSTOMER_ID), BookingRefusal.CUSTOMER_SUSPENDED);
    }

    @Test
    @Tag("RES-1")
    @DisplayName("Aisyah in Salak Tinggi can reserve Kemas Home. Her sister in Shah Alam can't.")
    void customerOutsideTheProvidersCitiesIsRefused() {
        assertRefused(() -> start(SHAH_ALAM_CUSTOMER_ID), BookingRefusal.CITY_NOT_COVERED);
    }

    @Test
    @Tag("RES-9")
    @DisplayName("Once Aisyah's request blocks Saturday 9:45–14:30, Mei can't start paying for 10:00.")
    void timeAlreadyBlockedIsRefusedAtCheckout() {
        bed.requestStandard();
        bed.publisher.clear();
        int checkouts = bed.checkouts.all().size();

        assertThatThrownBy(() -> start(MEI_CUSTOMER_ID)).isInstanceOfSatisfying(BookingRefusedException.class,
                refused -> assertThat(refused.refusal()).isEqualTo(BookingRefusal.TIME_TAKEN_BY_ANOTHER_RESERVATION));
        assertThat(bed.checkouts.all()).hasSize(checkouts);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("RES-10")
    @DisplayName("Aisyah can't also start paying another provider for Saturday 12:00 while she has a job then.")
    void customerWithAJobAtThatTimeIsRefused() {
        bed.requestStandard();
        bed.listInstantProvider();
        bed.publisher.clear();

        assertThatThrownBy(() -> bed.startCheckout.start(new StartReservationCheckoutRequest(AISYAH_CUSTOMER_ID,
                ReservationTestBed.INSTANT_PROVIDER_ID, List.of(KITCHEN_CLEANING), ReservationTestBed.saturday(12, 0),
                120, 1)))
                .isInstanceOfSatisfying(BookingRefusedException.class,
                        refused -> assertThat(refused.refusal()).isEqualTo(BookingRefusal.CUSTOMER_BUSY));
    }

    @Test
    @Tag("RES-2")
    @DisplayName("A checkout needs a registered customer and a listed provider.")
    void unknownCustomerOrProviderIsNotFound() {
        assertThatThrownBy(() -> start("customer-unknown")).isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> bed.startCheckout.start(standardCheckout(AISYAH_CUSTOMER_ID, "provider-unknown")))
                .isInstanceOf(ProviderNotFoundException.class);
    }
}
