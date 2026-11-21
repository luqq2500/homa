package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_TIME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.domain.ReservationFixtures.MEI_CUSTOMER_ID;
import static com.homa.reservation.domain.ReservationFixtures.TAPPED_PAY_AT;
import static com.homa.reservation.domain.ReservationFixtures.anotherCustomerBooking;
import static com.homa.reservation.domain.ReservationFixtures.assertRefused;
import static com.homa.reservation.domain.ReservationFixtures.meiBooking;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.domain.schedule.ScheduleBlock;
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
        Reservation aisyahs = market.requestStandard();

        assertThat(market.providerSchedule().isFree(STANDARD_BLOCK)).isFalse();
        assertThat(market.providerSchedule().blocks()).containsExactly(new ScheduleBlock(aisyahs.id(), STANDARD_BLOCK));
        assertRefused(() -> market.checkout(anotherCustomerBooking(saturday(11, 0), Duration.ofHours(2), 1),
                monday(21, 0)), BookingRefusal.TIME_TAKEN_BY_ANOTHER_RESERVATION);
        assertRefused(() -> market.checkout(anotherCustomerBooking(saturday(14, 30), Duration.ofHours(2), 1),
                monday(21, 0)), BookingRefusal.TIME_TAKEN_BY_ANOTHER_RESERVATION);
        Reservation afterAisyah = market.book(anotherCustomerBooking(saturday(14, 45), Duration.ofHours(2), 1),
                monday(21, 0), monday(21, 1));
        assertThat(afterAisyah.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(market.providerSchedule().blocks()).hasSize(2);
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah and Mei both pay for Kemas Home on Saturday at 10:00. Aisyah's payment confirms at 20:01 and Mei's at 20:02. Aisyah's reservation is Requested; Mei gets her RM200 back straight away and is told the slot was taken.")
    void firstConfirmedPaymentForATimeWins() {
        Checkout aisyahsCheckout = market.checkout(standardBooking(), TAPPED_PAY_AT);
        Checkout meisCheckout = market.checkout(meiBooking(), monday(19, 59));

        Reservation aisyahs = market.pay(aisyahsCheckout, monday(20, 1));

        assertThat(aisyahs.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThatThrownBy(() -> market.pay(meisCheckout, monday(20, 2)))
                .isInstanceOfSatisfying(BookingRefusedException.class, refused -> {
                    assertThat(refused.refusal()).isEqualTo(BookingRefusal.TIME_TAKEN_BY_ANOTHER_RESERVATION);
                    assertThat(refused.rejection()).isEqualTo(CheckoutRejection.TIME_TAKEN);
                });
        assertThat(meisCheckout.quote().total()).isEqualTo(aisyahs.quote().total());
        assertThat(market.providerSchedule().blocks()).containsExactly(new ScheduleBlock(aisyahs.id(), STANDARD_BLOCK));
        assertThat(market.scheduleOf(MEI_CUSTOMER_ID).isFree(JOB_TIME)).isTrue();
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Starting a checkout holds nothing: while Aisyah pays, Kemas Home's Saturday stays free and Mei can start paying for it too.")
    void checkoutHoldsNothing() {
        market.checkout(standardBooking(), TAPPED_PAY_AT);

        assertThat(market.providerSchedule().isFree(STANDARD_BLOCK)).isTrue();
        assertThat(market.providerSchedule().blocks()).isEmpty();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).blocks()).isEmpty();
        assertThat(market.checkout(meiBooking(), monday(19, 59))).isNotNull();
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah's and Mei's payments are confirmed against the same saved schedule at once. Both see the same version, so only the first save may stand; reloaded after Aisyah's, Mei's payment is refused.")
    void paymentsConfirmedAgainstTheSameVersionAreDecidedByTheFirstSave() {
        ProviderSchedule saved = new ProviderSchedule(KEMAS_HOME_PROVIDER_ID, List.of(), 3);
        ProviderSchedule seenByAisyah = new ProviderSchedule(KEMAS_HOME_PROVIDER_ID, saved.blocks(), saved.version());
        ProviderSchedule seenByMei = new ProviderSchedule(KEMAS_HOME_PROVIDER_ID, saved.blocks(), saved.version());

        ReservationId aisyahs = ReservationId.generate();
        seenByAisyah.block(aisyahs, STANDARD_BLOCK);
        seenByMei.block(ReservationId.generate(), STANDARD_BLOCK);
        assertThat(seenByAisyah.version()).isEqualTo(3).isEqualTo(seenByMei.version());

        ProviderSchedule reloadedForMei =
                new ProviderSchedule(KEMAS_HOME_PROVIDER_ID, seenByAisyah.blocks(), seenByAisyah.version() + 1);
        assertThat(reloadedForMei.version()).isEqualTo(4);
        assertRefused(() -> reloadedForMei.block(ReservationId.generate(), STANDARD_BLOCK),
                BookingRefusal.TIME_TAKEN_BY_ANOTHER_RESERVATION);
        assertThat(reloadedForMei.blocks()).containsExactly(new ScheduleBlock(aisyahs, STANDARD_BLOCK));
    }

    @Test
    @Tag("RES-9")
    @DisplayName("A saved schedule never holds two overlapping blocks.")
    void savedScheduleCannotHoldOverlappingBlocks() {
        List<ScheduleBlock> overlapping = List.of(new ScheduleBlock(ReservationId.generate(), STANDARD_BLOCK),
                new ScheduleBlock(ReservationId.generate(), new TimeInterval(saturday(14, 0), saturday(16, 0))));

        assertRefused(() -> new ProviderSchedule(KEMAS_HOME_PROVIDER_ID, overlapping, 1),
                BookingRefusal.TIME_TAKEN_BY_ANOTHER_RESERVATION);
        assertThat(new ProviderSchedule(KEMAS_HOME_PROVIDER_ID, overlapping.subList(0, 1), 1).isFree(STANDARD_BLOCK))
                .isFalse();
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
        market.scheduleOf(AISYAH_CUSTOMER_ID).release(reservation);

        assertThat(schedule.isFree(STANDARD_BLOCK)).isTrue();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).isFree(JOB_TIME)).isTrue();
    }

    @Test
    @Tag("RES-9")
    @DisplayName("Releasing one reservation leaves another customer's reservation in place.")
    void releasingAReservationKeepsOtherBlocks() {
        Reservation aisyahs = market.reserveStandard();
        Reservation later = market.book(anotherCustomerBooking(saturday(15, 0), Duration.ofHours(2), 1),
                monday(21, 0), monday(21, 1));
        aisyahs.markCompleted(List.of(new CompletionPhoto("photo-kitchen")), JOB_END);

        market.providerSchedule().release(aisyahs);

        assertThat(market.providerSchedule().blocks()).extracting(ScheduleBlock::reservation)
                .containsExactly(later.id());
    }

    @Test
    @DisplayName("A provider's schedule belongs to the provider and starts empty at version 0.")
    void scheduleBelongsToItsProvider() {
        assertThat(market.providerSchedule().providerId()).isEqualTo(KEMAS_HOME_PROVIDER_ID);
        assertThat(market.providerSchedule().version()).isZero();
    }
}
