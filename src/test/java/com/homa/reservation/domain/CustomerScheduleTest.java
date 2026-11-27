package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_TIME;
import static com.homa.reservation.domain.ReservationFixtures.ANOTHER_PROVIDER_ID;
import static com.homa.reservation.domain.ReservationFixtures.TAPPED_PAY_AT;
import static com.homa.reservation.domain.ReservationFixtures.aisyahBooking;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static com.homa.reservation.domain.ReservationFixtures.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.replica.DailyHours;
import com.homa.reservation.domain.replica.WorkingHours;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ScheduleBlock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
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
    @DisplayName("Aisyah can't also request another provider for Saturday 12:00, but can book Sunday.")
    void customerCannotHaveTwoReservationsWithOverlappingJobTimes() {
        Reservation kemas = kemasHome.requestStandard();

        assertRefused(() -> checkoutWithAnotherProvider(saturday(12, 0)), BookingRefusal.CUSTOMER_BUSY);
        assertThat(checkoutWithAnotherProvider(sunday(12, 0))).isNotNull();
        assertThat(aisyahsSchedule.blocks()).containsExactly(new ScheduleBlock(kemas.id(), JOB_TIME));
    }

    @Test
    @Tag("RES-10")
    @DisplayName("The customer's calendar holds only the job time, without check-in or buffer, so a job from 14:00 is fine.")
    void customersCalendarBlocksTheJobTimeOnly() {
        kemasHome.requestStandard();

        assertThat(aisyahsSchedule.isFree(new TimeInterval(saturday(13, 59), saturday(14, 0)))).isFalse();
        assertThat(checkoutWithAnotherProvider(saturday(14, 0))).isNotNull();
    }

    @Test
    @Tag("RES-10")
    @DisplayName("Aisyah pays for Kemas Home and for another provider at the same Saturday time. The other provider's payment confirms first, so Kemas Home's is refused as a failed rule and Kemas Home's time stays free.")
    void laterConfirmedPaymentForAnOverlappingJobIsRefused() {
        Checkout kemas = kemasHome.checkout(standardBooking(), TAPPED_PAY_AT);
        Checkout other = checkoutWithAnotherProvider(saturday(10, 0));
        Reservation.request(ReservationId.generate(), other, anotherProvider.provider(), ReservationFixtures.aisyah(),
                anotherProvider.providerSchedule(), aisyahsSchedule, monday(20, 1));

        assertThatThrownBy(() -> kemasHome.pay(kemas, monday(20, 2)))
                .isInstanceOfSatisfying(BookingRefusedException.class, refused -> {
                    assertThat(refused.refusal()).isEqualTo(BookingRefusal.CUSTOMER_BUSY);
                    assertThat(refused.rejection()).isEqualTo(CheckoutRejection.RULE_FAILED);
                });
        assertThat(kemasHome.providerSchedule().blocks()).isEmpty();
    }

    @Test
    @Tag("RES-10")
    @DisplayName("A customer's schedule as it was saved keeps its blocks and version.")
    void savedCustomerScheduleKeepsItsBlocksAndVersion() {
        ScheduleBlock saturdayJob = new ScheduleBlock(ReservationId.generate(), JOB_TIME);
        CustomerSchedule saved = new CustomerSchedule(AISYAH_CUSTOMER_ID, List.of(saturdayJob), 5);

        assertThat(saved.blocks()).containsExactly(saturdayJob);
        assertThat(saved.version()).isEqualTo(5);
        assertRefused(() -> saved.requireFree(new TimeInterval(saturday(12, 0), saturday(13, 0))),
                BookingRefusal.CUSTOMER_BUSY);
    }

    @Test
    @Tag("RES-10")
    @DisplayName("A customer's schedule belongs to the customer.")
    void scheduleBelongsToItsCustomer() {
        assertThat(aisyahsSchedule.customerId()).isEqualTo(AISYAH_CUSTOMER_ID);
        assertThat(aisyahsSchedule.version()).isZero();
    }

    private Checkout checkoutWithAnotherProvider(Instant start) {
        Booking booking = aisyahBooking(start, Duration.ofHours(2), 1);
        Booking withAnother = new Booking(booking.customerId(), ANOTHER_PROVIDER_ID, booking.jobs(), booking.jobTime(),
                booking.helpers(), booking.address());
        return Checkout.start(CheckoutId.generate(), withAnother, anotherProvider.provider(),
                ReservationFixtures.aisyah(), anotherProvider.providerSchedule(), aisyahsSchedule, monday(19, 0));
    }
}
