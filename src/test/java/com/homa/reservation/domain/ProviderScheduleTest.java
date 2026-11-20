package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.domain.ReservationFixtures.TAPPED_PAY_AT;
import static com.homa.reservation.domain.ReservationFixtures.anotherCustomerBooking;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderScheduleTest {

    private static final TimeInterval STANDARD_BLOCK = new TimeInterval(saturday(9, 45), saturday(14, 30));

    private final Marketplace market = Marketplace.withKemasHome();

    @Test
    @Tag("RES-9")
    @DisplayName("Aisyah's request for Saturday 10:00–14:00 blocks Kemas Home from 9:45 to 14:30. Another customer can't book Kemas Home at 11:00 on Saturday, even for 1 helper; the earliest start after Aisyah's job is 14:45.")
    void providerHasOnlyOneReservationAtATime() {
        market.requestStandard();

        assertThat(market.providerSchedule().isFree(STANDARD_BLOCK, REQUESTED_AT)).isFalse();
        assertRefused(() -> market.checkout(anotherCustomerBooking(saturday(11, 0), Duration.ofHours(2), 1),
                monday(21, 0)), BookingRefusal.PROVIDER_BUSY);
        assertRefused(() -> market.checkout(anotherCustomerBooking(saturday(14, 30), Duration.ofHours(2), 1),
                monday(21, 0)), BookingRefusal.PROVIDER_BUSY);
        Checkout afterAisyah = market.checkout(anotherCustomerBooking(saturday(14, 45), Duration.ofHours(2), 1),
                monday(21, 0));
        market.pay(afterAisyah, monday(21, 1));
        assertThat(market.providerSchedule().isFree(STANDARD_BLOCK, monday(21, 1))).isFalse();
    }

    @Test
    @Tag("RES-9")
    @DisplayName("Once the job is marked Completed, the time opens again; while it is Reserved it can't be released.")
    void providersTimeOpensAgainOnlyOnceTheReservationNoLongerHoldsIt() {
        Reservation reservation = market.reserveStandard();
        ProviderSchedule schedule = market.providerSchedule();

        assertThatThrownBy(() -> schedule.release(reservation)).isInstanceOf(ReservationStatusConflictException.class);

        reservation.markCompleted(List.of(new CompletionPhoto("photo-kitchen")), JOB_END);
        schedule.release(reservation);
        market.scheduleOf(reservation.booking().customerId()).release(reservation);

        assertThat(schedule.isFree(STANDARD_BLOCK, JOB_END)).isTrue();
        assertThat(market.scheduleOf(reservation.booking().customerId()).isFree(reservation.jobTime().interval(), JOB_END))
                .isTrue();
    }

    @Test
    @Tag("RES-9")
    @DisplayName("Releasing one reservation leaves another customer's live checkout hold in place.")
    void releasingAReservationKeepsOtherHolds() {
        Reservation reservation = market.reserveStandard();
        reservation.markCompleted(List.of(new CompletionPhoto("photo-kitchen")), JOB_END);
        TimeInterval later = new TimeInterval(saturday(15, 0), saturday(16, 0));
        market.providerSchedule().hold(CheckoutId.generate(), later, JOB_END);

        market.providerSchedule().release(reservation);

        assertThat(market.providerSchedule().isFree(later, JOB_END)).isFalse();
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah taps Pay at 19:58 and the slot is held until 20:13.")
    void checkoutHoldsTheProvidersTimeForFifteenMinutes() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);

        assertThat(checkout.holdExpiresAt()).isEqualTo(monday(20, 13));
        assertThat(market.providerSchedule().isHolding(checkout.id(), monday(20, 12))).isTrue();
        assertRefused(() -> market.checkout(anotherCustomerBooking(saturday(11, 0), Duration.ofHours(2), 1),
                monday(20, 12)), BookingRefusal.PROVIDER_BUSY);
    }

    @Test
    @Tag("RES-11")
    @DisplayName("If payment isn't secured within 15 minutes, the hold is released: at 20:13 another customer can book the slot.")
    void expiredCheckoutHoldNoLongerCounts() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);

        assertThat(market.providerSchedule().isHolding(checkout.id(), monday(20, 13))).isFalse();
        assertThat(market.checkout(anotherCustomerBooking(saturday(11, 0), Duration.ofHours(2), 1), monday(20, 13)))
                .isNotNull();
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Had the payment failed, another customer could book the slot from then.")
    void failedPaymentReleasesTheHoldAtOnce() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);

        market.providerSchedule().releaseHold(checkout.id());

        assertThat(market.providerSchedule().isHolding(checkout.id(), REQUESTED_AT)).isFalse();
        assertThat(market.checkout(anotherCustomerBooking(saturday(11, 0), Duration.ofHours(2), 1), REQUESTED_AT))
                .isNotNull();
    }

    @Test
    @DisplayName("A provider's schedule belongs to the provider.")
    void scheduleBelongsToItsProvider() {
        assertThat(market.providerSchedule().providerId()).isEqualTo(KEMAS_HOME_PROVIDER_ID);
    }
}
