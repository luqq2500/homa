package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.ACCEPTANCE_DEADLINE;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.HOURLY_RATE_PER_HELPER;
import static com.homa.fixtures.HomaFixtures.JOB_TIME;
import static com.homa.fixtures.HomaFixtures.PROTECTION_WINDOW;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.TAPPED_PAY_AT;
import static com.homa.reservation.domain.ReservationFixtures.aisyahAddress;
import static com.homa.reservation.domain.ReservationFixtures.aisyahBooking;
import static com.homa.reservation.domain.ReservationFixtures.anotherCustomerBooking;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static com.homa.reservation.domain.ReservationFixtures.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.schedule.ScheduleBlock;
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
    @DisplayName("Aisyah pays RM200 on Monday 20:00. Her reservation is Requested.")
    void confirmedPaymentMakesARequestedReservationWithThePaymentHeld() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);

        Reservation reservation = market.pay(checkout, REQUESTED_AT);

        assertThat(reservation.id()).isNotNull();
        assertThat(reservation.checkoutId()).isEqualTo(checkout.id());
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(reservation.requestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(reservation.booking()).isEqualTo(standardBooking());
        assertThat(reservation.reservedAt()).isEmpty();
        assertThat(reservation.withdrawnAt()).isEmpty();
        assertThat(reservation.roster()).isEmpty();
        assertThat(reservation.completedAt()).isEmpty();
        assertThat(reservation.completionPhotos()).isEmpty();
    }

    @Test
    @Tag("RES-7")
    @DisplayName("The Requested reservation tells its events when it was Requested, that its Check-in Period starts Saturday 9:45 and that its Protection Window ends Sunday 14:00.")
    void requestedReservationGivesItsEventsTheirTimes() {
        Reservation reservation = market.requestStandard();

        assertThat(reservation.requestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(reservation.checkInStart()).isEqualTo(saturday(9, 45));
        assertThat(reservation.protectionWindowEnd()).isEqualTo(PROTECTION_WINDOW.end());
        assertThat(reservation.values()).isEqualTo(ReservationValues.asOf(REQUESTED_AT));
    }

    @Test
    @Tag("RES-9")
    @DisplayName("From Monday 20:00 Kemas Home is blocked 9:45–14:30 and Aisyah 10:00–14:00, by her reservation.")
    void requestBlocksBothSchedules() {
        Reservation reservation = market.requestStandard();

        assertThat(market.providerSchedule().blocks())
                .containsExactly(new ScheduleBlock(reservation.id(), STANDARD_BLOCK));
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).blocks())
                .containsExactly(new ScheduleBlock(reservation.id(), JOB_TIME));
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Kemas Home raises its rate to RM30 and switches to Firm on Wednesday; Aisyah's Saturday booking stays RM200 under Standard.")
    void reservationKeepsTheTermsFromWhenItWasRequested() {
        Reservation reservation = market.requestStandard();

        market.providerBecomes(kemasHomeProvider().rate(Money.myr(30)).tier(CancellationTier.FIRM).build());

        assertThat(reservation.terms().hourlyRatePerHelper()).isEqualTo(HOURLY_RATE_PER_HELPER);
        assertThat(reservation.terms().cancellationTier()).isEqualTo(CancellationTier.STANDARD);
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        Reservation newBooking = market.book(
                anotherCustomerBooking(HomaFixtures.at(2026, 12, 8, 10, 0), Duration.ofHours(4), 2),
                tuesday(9, 0), tuesday(9, 1));
        assertThat(newBooking.quote().total()).isEqualTo(Money.myr(236));
        assertThat(newBooking.terms().cancellationTier()).isEqualTo(CancellationTier.FIRM);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Her new address on Thursday applies only to new bookings.")
    void reservationKeepsTheAddressFromWhenItWasRequested() {
        Reservation reservation = market.requestStandard();

        market.register(CustomerStanding.registered(AISYAH_CUSTOMER_ID,
                new ServiceAddress("8, Jalan Cyber 2", "CYBERJAYA", "Ring the bell")).verified());

        assertThat(reservation.booking().address()).isEqualTo(aisyahAddress());
        Checkout nextBooking = market.checkout(
                aisyahBooking(HomaFixtures.at(2026, 12, 8, 10, 0), Duration.ofHours(4), 2), tuesday(9, 0));
        assertThat(nextBooking.booking().address().city()).isEqualTo("CYBERJAYA");
    }

    @Test
    @Tag("RES-8")
    @DisplayName("The terms and address are frozen when the payment confirms, not when Aisyah tapped Pay: Kemas Home switching to Firm and Aisyah moving within Salak Tinggi at 19:59 both apply to her reservation.")
    void termsAndAddressAreFrozenWhenTheReservationIsRequested() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);
        ServiceAddress moved = new ServiceAddress("1, Jalan Salak Tinggi 3", HomaFixtures.AISYAH_CITY, "Side gate");
        market.providerBecomes(kemasHomeProvider().tier(CancellationTier.FIRM).build());
        market.register(CustomerStanding.registered(AISYAH_CUSTOMER_ID, moved).verified());

        Reservation reservation = market.pay(checkout, REQUESTED_AT);

        assertThat(reservation.terms().cancellationTier()).isEqualTo(CancellationTier.FIRM);
        assertThat(reservation.booking().address()).isEqualTo(moved);
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Kemas Home raises its rate to RM30 while Aisyah pays. Her confirmed RM200 no longer matches the RM236 price, so it is refused as a failed rule and refunded, and nothing is blocked.")
    void confirmedPaymentIsRefusedWhenThePriceChangedWhileTheCustomerPaid() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);
        market.providerBecomes(kemasHomeProvider().rate(Money.myr(30)).build());

        assertThatThrownBy(() -> market.pay(checkout, REQUESTED_AT))
                .isInstanceOfSatisfying(BookingRefusedException.class, refused -> {
                    assertThat(refused.refusal()).isEqualTo(BookingRefusal.PRICE_CHANGED);
                    assertThat(refused.rejection()).isEqualTo(CheckoutRejection.RULE_FAILED);
                });
        assertThat(market.providerSchedule().blocks()).isEmpty();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).blocks()).isEmpty();
    }

    @Test
    @Tag("RES-11")
    @DisplayName("A confirmed payment must still pass every booking rule: Aisyah taps Pay at 7:44 for 10:00; confirmed at 7:45 the 2-hour notice still holds, confirmed at 7:46 it no longer does.")
    void confirmedPaymentMustStillPassEveryBookingRule() {
        Checkout inTime = market.checkout(standardBooking(), saturday(7, 44));
        assertThat(market.pay(inTime, saturday(7, 45)).acceptanceDeadline())
                .contains(new AcceptanceDeadline(saturday(8, 45)));

        Marketplace again = Marketplace.withKemasHome();
        Checkout late = again.checkout(standardBooking(), saturday(7, 44));
        assertThatThrownBy(() -> again.pay(late, saturday(7, 46)))
                .isInstanceOfSatisfying(BookingRefusedException.class, refused -> {
                    assertThat(refused.refusal()).isEqualTo(BookingRefusal.NOTICE_TOO_SHORT);
                    assertThat(refused.rejection()).isEqualTo(CheckoutRejection.RULE_FAILED);
                });
        assertThat(again.providerSchedule().blocks()).isEmpty();
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah moves to Shah Alam while she pays. Kemas Home doesn't cover it, so her confirmed payment is refused.")
    void confirmedPaymentIsRefusedWhenTheHomeAddressMovedOutOfTheProvidersCities() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);
        market.register(CustomerStanding.registered(AISYAH_CUSTOMER_ID,
                new ServiceAddress("12, Jalan Shah Alam", "SHAH_ALAM", "Ring twice")).verified());

        assertRefused(() -> market.pay(checkout, REQUESTED_AT), BookingRefusal.CITY_NOT_COVERED);
    }

    @Test
    @Tag("CUS-5")
    @DisplayName("Aisyah is suspended while she pays, so her confirmed payment is refused.")
    void confirmedPaymentIsRefusedWhenTheCustomerWasSuspendedMeanwhile() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);
        market.register(new CustomerStanding(AISYAH_CUSTOMER_ID, true, true, aisyahAddress()));

        assertRefused(() -> market.pay(checkout, REQUESTED_AT), BookingRefusal.CUSTOMER_SUSPENDED);
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("Only identity-verified customers can make reservations: a confirmed payment from a customer Reservation doesn't know as verified is refused as a failed rule and refunded, and nothing is blocked.")
    void confirmedPaymentIsRefusedWhenTheCustomerIsNotVerified() {
        Checkout checkout = market.checkout(standardBooking(), TAPPED_PAY_AT);
        market.register(CustomerStanding.registered(AISYAH_CUSTOMER_ID, aisyahAddress()));

        assertThatThrownBy(() -> market.pay(checkout, REQUESTED_AT))
                .isInstanceOfSatisfying(BookingRefusedException.class, refused -> {
                    assertThat(refused.refusal()).isEqualTo(BookingRefusal.CUSTOMER_NOT_VERIFIED);
                    assertThat(refused.rejection()).isEqualTo(CheckoutRejection.RULE_FAILED);
                });
        assertThat(market.providerSchedule().blocks()).isEmpty();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).blocks()).isEmpty();
    }

    @Test
    @Tag("ACC-1")
    @DisplayName("With an Instant provider, Aisyah's booking is Reserved the moment she pays.")
    void instantReservationIsReservedAtOnce() {
        market.providerBecomes(kemasHomeProvider().policy(AcceptancePolicy.INSTANT).build());

        Reservation reservation = market.requestStandard();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.reservedAt()).contains(REQUESTED_AT);
        assertThat(reservation.acceptanceDeadline()).isEmpty();
    }

    @Test
    @Tag("ACC-1")
    @DisplayName("An Instant payment must be confirmed before the Check-in Period: 9:44 is Reserved, 9:45 is refused.")
    void instantPaymentMustBeConfirmedBeforeCheckIn() {
        market.providerBecomes(kemasHomeProvider().policy(AcceptancePolicy.INSTANT).minimumNotice(Duration.ZERO).build());
        assertThat(market.book(standardBooking(), saturday(9, 40), saturday(9, 44)).status())
                .isEqualTo(ReservationStatus.RESERVED);

        Marketplace again = new Marketplace(market.provider());
        Checkout checkout = again.checkout(standardBooking(), saturday(9, 40));
        assertRefused(() -> again.pay(checkout, saturday(9, 45)), BookingRefusal.TOO_LATE_FOR_ACCEPTANCE_POLICY);
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("A Monday 20:00 request has until Tuesday 20:00. A Saturday 7:00 request for 10:00 has until 8:45.")
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
    @DisplayName("A Request must be confirmed at least 1 hour before the Check-in Period: 8:45 is in time, 8:46 is refused.")
    void requestPaymentMustBeConfirmedAnHourBeforeCheckIn() {
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
        ReservationValues.Version values = ReservationValues.current();

        assertThat(AcceptanceDeadline.from(requestedAt, saturdayJob, values).at()).isEqualTo(saturday(8, 45));
        assertThat(AcceptanceDeadline.from(requestedAt.minusSeconds(60), saturdayJob, values).at())
                .isEqualTo(saturday(8, 44));
        assertThat(AcceptanceDeadline.from(requestedAt.plusSeconds(60), saturdayJob, values).at())
                .isEqualTo(saturday(8, 45));
    }
}
