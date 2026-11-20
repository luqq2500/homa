package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.ACCEPTANCE_DEADLINE;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.ANOTHER_PROVIDER_ID;
import static com.homa.reservation.domain.ReservationFixtures.TAPPED_PAY_AT;
import static com.homa.reservation.domain.ReservationFixtures.anotherCustomerBooking;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static com.homa.reservation.domain.ReservationFixtures.tuesday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.terms.AcceptanceDeadline;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import com.homa.reservation.domain.terms.CancellationTier;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReservationRequestTest {

    private static final TimeInterval STANDARD_BLOCK = new TimeInterval(saturday(9, 45), saturday(14, 30));

    private final Marketplace market = Marketplace.withKemasHome();

    @Test
    @Tag("RES-7")
    @DisplayName("Aisyah pays RM200 on Monday 20:00. The reservation is Requested and Homa holds the RM200.")
    void paidCheckoutBecomesARequestedReservationWithThePaymentHeld() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);

        Reservation reservation = market.pay(checkout, REQUESTED_AT);

        assertThat(reservation.id()).isNotNull();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(reservation.requestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(reservation.booking()).isEqualTo(standardBooking());
        assertThat(reservation.reservedAt()).isEmpty();
        assertThat(reservation.roster()).isEmpty();
        assertThat(reservation.completedAt()).isEmpty();
        assertThat(reservation.completionPhotos()).isEmpty();
    }

    @Test
    @Tag("RES-9")
    @DisplayName("From Monday 20:00 the checkout holds are the reservation's blocks, and they no longer expire at 20:13.")
    void requestTurnsTheCheckoutHoldsIntoBlocks() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);
        market.pay(checkout, REQUESTED_AT);

        assertThat(market.providerSchedule().isHolding(checkout.id(), REQUESTED_AT)).isFalse();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).isHolding(checkout.id(), REQUESTED_AT)).isFalse();
        assertThat(market.providerSchedule().isFree(STANDARD_BLOCK, monday(20, 13))).isFalse();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).isFree(HomaFixtures.JOB_TIME, monday(20, 13))).isFalse();
    }

    @Test
    @Tag("RES-8")
    @DisplayName("On Wednesday Kemas Home raises its rate to RM30 and switches to Firm. Aisyah's Saturday reservation stays at RM200 under Standard.")
    void reservationKeepsTheTermsFromWhenItWasRequested() {
        Reservation reservation = market.requestStandard();

        market.providerBecomes(kemasHomeProvider().rate(Money.myr(30)).tier(CancellationTier.FIRM).build());

        assertThat(reservation.terms().hourlyRatePerHelper()).isEqualTo(HOURLY_RATE_PER_HELPER);
        assertThat(reservation.terms().cancellationTier()).isEqualTo(CancellationTier.STANDARD);
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        Checkout newBooking = market.checkout(
                anotherCustomerBooking(HomaFixtures.at(2026, 12, 8, 10, 0), Duration.ofHours(4), 2), tuesday(9, 0));
        assertThat(newBooking.quote().total()).isEqualTo(Money.myr(236));
        assertThat(newBooking.terms().cancellationTier()).isEqualTo(CancellationTier.FIRM);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Aisyah's online banking confirms at 20:20, after the hold ended at 20:13. The slot is still free, so her reservation is Requested. Had another customer booked it at 20:15, Aisyah would get RM200 back straight away.")
    void latePaymentIsRequestedOnlyIfTheTimeIsStillFree() {
        Checkout late = market.checkout(standardBooking(), TAPPED_PAY_AT);
        Reservation reservation = market.pay(late, monday(20, 20));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(reservation.requestedAt()).isEqualTo(monday(20, 20));
        assertThat(reservation.acceptanceDeadline()).contains(new AcceptanceDeadline(tuesday(20, 20)));

        Marketplace taken = Marketplace.withKemasHome();
        Checkout aisyahs = taken.checkout(standardBooking(), TAPPED_PAY_AT);
        taken.checkout(anotherCustomerBooking(saturday(11, 0), Duration.ofHours(2), 1), monday(20, 15));
        assertRefused(() -> taken.pay(aisyahs, monday(20, 20)), BookingRefusal.PROVIDER_BUSY);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Paid at 20:12 the hold is still live; paid at 20:13 it has expired and another customer who took the slot at 20:13 keeps it.")
    void holdIsLiveUntilTheMinuteItExpires() {
        Checkout live = market.checkout(standardBooking(), TAPPED_PAY_AT);
        assertThat(market.pay(live, monday(20, 12)).status()).isEqualTo(ReservationStatus.REQUESTED);

        Marketplace taken = Marketplace.withKemasHome();
        Checkout expired = taken.checkout(standardBooking(), TAPPED_PAY_AT);
        taken.checkout(anotherCustomerBooking(saturday(11, 0), Duration.ofHours(2), 1), monday(20, 13));
        assertRefused(() -> taken.pay(expired, monday(20, 13)), BookingRefusal.PROVIDER_BUSY);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("A late payment must still pass every booking rule: held at 7:44 for 10:00, paid at 8:00 the 2-hour notice no longer holds; paid at 7:58 the live hold stands.")
    void latePaymentMustStillPassEveryBookingRule() {
        Checkout live = market.checkout(standardBooking(), saturday(7, 44));
        assertThat(market.pay(live, saturday(7, 58)).acceptanceDeadline())
                .contains(new AcceptanceDeadline(saturday(8, 45)));

        Marketplace again = Marketplace.withKemasHome();
        Checkout late = again.checkout(standardBooking(), saturday(7, 44));
        assertRefused(() -> again.pay(late, saturday(8, 0)), BookingRefusal.NOTICE_TOO_SHORT);
    }

    @Test
    @Tag("RES-12")
    @DisplayName("Aisyah booked another provider for the same time after her hold expired, so her late payment is refunded and Kemas Home's time stays free.")
    void refusedLatePaymentLeavesTheProvidersTimeFree() {
        Checkout kemasCheckout = market.checkout(standardBooking(), TAPPED_PAY_AT);
        Marketplace another = new Marketplace(kemasHomeProvider().id(ANOTHER_PROVIDER_ID).build());
        Booking sameTime = new Booking(AISYAH_CUSTOMER_ID, ANOTHER_PROVIDER_ID, standardBooking().jobs(),
                standardBooking().jobTime(), 2, standardBooking().address());
        Checkout anotherCheckout = Checkout.start(CheckoutId.generate(), sameTime, another.provider(),
                ReservationFixtures.aisyah(), another.providerSchedule(), market.scheduleOf(AISYAH_CUSTOMER_ID),
                monday(20, 15));
        Reservation.request(ReservationId.generate(), anotherCheckout, another.provider(), ReservationFixtures.aisyah(),
                another.providerSchedule(), market.scheduleOf(AISYAH_CUSTOMER_ID), monday(20, 16));

        assertRefused(() -> market.pay(kemasCheckout, monday(20, 20)), BookingRefusal.CUSTOMER_BUSY);
        assertThat(market.providerSchedule().isFree(STANDARD_BLOCK, monday(20, 20))).isTrue();
    }

    @Test
    @Tag("ACC-1")
    @DisplayName("An Instant provider: Aisyah pays and the reservation is Reserved at once.")
    void instantReservationIsReservedAtOnce() {
        market.providerBecomes(kemasHomeProvider().policy(AcceptancePolicy.INSTANT).build());

        Reservation reservation = market.requestStandard();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.reservedAt()).contains(REQUESTED_AT);
        assertThat(reservation.acceptanceDeadline()).isEmpty();
    }

    @Test
    @Tag("ACC-1")
    @DisplayName("An Instant payment that lands while the hold is live must still come before the Check-in Period: 9:44 is Reserved, 9:45 is refused.")
    void instantPaymentMustLandBeforeCheckIn() {
        market.providerBecomes(kemasHomeProvider().policy(AcceptancePolicy.INSTANT).minimumNotice(Duration.ZERO).build());
        assertThat(market.book(standardBooking(), saturday(9, 40), saturday(9, 44)).status())
                .isEqualTo(ReservationStatus.RESERVED);

        Marketplace again = new Marketplace(market.provider());
        Checkout checkout = again.checkout(standardBooking(), saturday(9, 40));
        assertRefused(() -> again.pay(checkout, saturday(9, 45)), BookingRefusal.TOO_LATE_FOR_ACCEPTANCE_POLICY);
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("Aisyah requests on Monday 20:00 for Saturday, so Kemas Home must respond by Tuesday 20:00. A request made on Saturday 7:00 for 10:00 has a deadline of 8:45. A request at 8:00 for 10:00 is blocked, because Kemas Home's 2-hour notice needs it by 7:45.")
    void acceptanceDeadlineIsADayLaterOrAnHourBeforeCheckInWhicheverComesFirst() {
        assertThat(market.requestStandard().acceptanceDeadline()).contains(new AcceptanceDeadline(ACCEPTANCE_DEADLINE));

        Marketplace saturdayMorning = Marketplace.withKemasHome();
        assertThat(saturdayMorning.book(standardBooking(), saturday(6, 59), saturday(7, 0)).acceptanceDeadline())
                .contains(new AcceptanceDeadline(saturday(8, 45)));

        assertRefused(() -> Marketplace.withKemasHome().checkout(standardBooking(), saturday(8, 0)),
                BookingRefusal.NOTICE_TOO_SHORT);
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("A Request paid while its hold is live must still be made at least 1 hour before the Check-in Period: 8:45 is in time, 8:46 is refused.")
    void requestPaymentMustLandAnHourBeforeCheckIn() {
        market.providerBecomes(kemasHomeProvider().minimumNotice(Duration.ofMinutes(30)).build());
        assertThat(market.book(standardBooking(), saturday(8, 40), saturday(8, 45)).status())
                .isEqualTo(ReservationStatus.REQUESTED);

        Marketplace again = new Marketplace(market.provider());
        Checkout checkout = again.checkout(standardBooking(), saturday(8, 40));
        assertRefused(() -> again.pay(checkout, saturday(8, 46)), BookingRefusal.TOO_LATE_FOR_ACCEPTANCE_POLICY);
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("Requested exactly 25 hours before the Check-in Period, both limits fall on the same minute.")
    void acceptanceDeadlineWhenBothLimitsCoincide() {
        JobTime saturdayJob = standardBooking().jobTime();
        Instant requestedAt = saturday(9, 45).minus(Duration.ofHours(25));

        assertThat(AcceptanceDeadline.from(requestedAt, saturdayJob).at()).isEqualTo(saturday(8, 45));
        assertThat(AcceptanceDeadline.from(requestedAt.minusSeconds(60), saturdayJob).at())
                .isEqualTo(saturday(8, 44));
        assertThat(AcceptanceDeadline.from(requestedAt.plusSeconds(60), saturdayJob).at())
                .isEqualTo(saturday(8, 45));
    }
}
