package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.reservation.domain.ReservationFixtures.ANOTHER_PROVIDER_ID;
import static com.homa.reservation.domain.ReservationFixtures.aisyahBooking;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.sunday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.replica.DailyHours;
import com.homa.reservation.domain.replica.WorkingHours;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerScheduleTest {

    private final Marketplace kemasHome = Marketplace.withKemasHome();
    private final CustomerSchedule aisyahsSchedule = kemasHome.scheduleOf(AISYAH_CUSTOMER_ID);
    private final Marketplace anotherProvider = new Marketplace(kemasHomeProvider().id(ANOTHER_PROVIDER_ID)
            .workingHours(new WorkingHours(Map.of(
                    DayOfWeek.SATURDAY, new DailyHours(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                    DayOfWeek.SUNDAY, new DailyHours(LocalTime.of(8, 0), LocalTime.of(20, 0)))))
            .build());

    @Test
    @Tag("RES-10")
    @DisplayName("Aisyah has requested Kemas Home for Saturday 10:00–14:00. She can't also request another provider for Saturday 12:00. She can book Sunday.")
    void customerCannotHaveTwoReservationsWithOverlappingJobTimes() {
        kemasHome.requestStandard();

        assertRefused(() -> checkoutWithAnotherProvider(saturday(12, 0)), BookingRefusal.CUSTOMER_BUSY);
        assertThat(checkoutWithAnotherProvider(sunday(12, 0))).isNotNull();
    }

    @Test
    @Tag("RES-10")
    @DisplayName("The customer's calendar holds only the job time, without check-in or buffer, so a job from 14:00 is fine.")
    void customersCalendarBlocksTheJobTimeOnly() {
        kemasHome.requestStandard();

        assertThat(aisyahsSchedule.isFree(new TimeInterval(saturday(13, 59), saturday(14, 0)), monday(21, 0))).isFalse();
        assertThat(checkoutWithAnotherProvider(saturday(14, 0))).isNotNull();
    }

    @Test
    @Tag("RES-10")
    @DisplayName("When Aisyah is already busy, her checkout with another provider leaves that provider's time untouched.")
    void refusedCheckoutLeavesTheProvidersTimeFree() {
        kemasHome.requestStandard();

        assertRefused(() -> checkoutWithAnotherProvider(saturday(12, 0)), BookingRefusal.CUSTOMER_BUSY);

        assertThat(anotherProvider.providerSchedule().isFree(new TimeInterval(saturday(11, 45), saturday(14, 30)),
                monday(21, 0))).isTrue();
    }

    @Test
    @DisplayName("A customer's schedule belongs to the customer.")
    void scheduleBelongsToItsCustomer() {
        assertThat(aisyahsSchedule.customerId()).isEqualTo(AISYAH_CUSTOMER_ID);
    }

    private Checkout checkoutWithAnotherProvider(Instant start) {
        Booking booking = aisyahBooking(start, Duration.ofHours(2), 1);
        Booking withAnother = new Booking(booking.customerId(), ANOTHER_PROVIDER_ID, booking.jobs(), booking.jobTime(),
                booking.helpers(), booking.address());
        return Checkout.start(CheckoutId.generate(), withAnother, anotherProvider.provider(),
                ReservationFixtures.aisyah(), anotherProvider.providerSchedule(), aisyahsSchedule, monday(21, 0));
    }
}
