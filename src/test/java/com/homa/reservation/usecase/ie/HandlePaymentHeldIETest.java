package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.ACCEPTANCE_DEADLINE;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.INSTANT_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.monday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.events.ReservationCheckoutRejectedIE;
import com.homa.ie.events.ReservationRequestedIE;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.kernel.Money;
import com.homa.kernel.TimeInterval;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.schedule.ScheduleBlock;
import com.homa.reservation.usecase.ReservationTestBed;
import com.homa.reservation.usecase.exception.CheckoutNotFoundException;
import com.homa.reservation.usecase.exception.CustomerNotFoundException;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandlePaymentHeldIETest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private PaymentHeldIE paid(String checkoutId) {
        bed.time.set(REQUESTED_AT);
        return bed.paymentHeld(checkoutId, REQUESTED_AT);
    }

    @Test
    @Tag("RES-7")
    @DisplayName("Aisyah pays RM200 on Monday 20:00. Her reservation is Requested, and both schedules are blocked.")
    void confirmedPaymentMakesTheReservationRequested() {
        String checkoutId = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        bed.publisher.clear();

        bed.paymentHeld.handle(paid(checkoutId));

        Reservation reservation = bed.reservations.single();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(reservation.checkoutId().value()).isEqualTo(checkoutId);
        assertThat(reservation.requestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).containsExactly(
                new ScheduleBlock(reservation.id(), new TimeInterval(saturday(9, 45), saturday(14, 30))));
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).containsExactly(
                new ScheduleBlock(reservation.id(), new TimeInterval(JOB_START, JOB_END)));
        assertThat(bed.publisher.single(ReservationRequestedIE.class)).isEqualTo(new ReservationRequestedIE(
                "reservation-event-2", reservation.id().value(), checkoutId, AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, saturday(9, 45), JOB_START, JOB_END, Money.myr(200), ACCEPTANCE_DEADLINE,
                sunday(14, 0), REQUESTED_AT, REQUESTED_AT));
    }

    @Test
    @Tag("ACC-1")
    @DisplayName("With an Instant provider, Aisyah's booking is Reserved the moment she pays: Requested and Reserved are both published.")
    void instantReservationIsRequestedAndReservedAtOnce() {
        bed.listInstantProvider();
        String checkoutId = bed.startCheckout(AISYAH_CUSTOMER_ID, INSTANT_PROVIDER_ID);
        bed.publisher.clear();

        bed.paymentHeld.handle(paid(checkoutId));

        Reservation reservation = bed.reservations.single();
        String id = reservation.id().value();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.publisher.published()).containsExactly(
                new ReservationRequestedIE("reservation-event-2", id, checkoutId, AISYAH_CUSTOMER_ID,
                        INSTANT_PROVIDER_ID, saturday(9, 45), JOB_START, JOB_END, Money.myr(200), null,
                        sunday(14, 0), REQUESTED_AT, REQUESTED_AT),
                new ReservationReservedIE("reservation-event-3", id, AISYAH_CUSTOMER_ID, INSTANT_PROVIDER_ID,
                        saturday(9, 45), JOB_START, JOB_END, Money.myr(200), sunday(14, 0), REQUESTED_AT,
                        REQUESTED_AT));
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Aisyah's payment confirms at 20:01 and Mei's at 20:02. Aisyah's is Requested; Mei gets RM200 back straight away.")
    void laterPaymentForTheSameTimeIsRejected() {
        String aisyahs = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        String meis = bed.startStandardCheckout(MEI_CUSTOMER_ID);
        bed.time.set(monday(20, 1));
        bed.paymentHeld.handle(bed.paymentHeld(aisyahs, monday(20, 1)));
        bed.publisher.clear();
        bed.time.set(monday(20, 2));

        bed.paymentHeld.handle(bed.paymentHeld(meis, monday(20, 2)));

        assertThat(bed.reservations.count()).isEqualTo(1);
        assertThat(bed.reservations.single().booking().customerId()).isEqualTo(AISYAH_CUSTOMER_ID);
        assertThat(bed.schedules.customerBlocks(MEI_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationCheckoutRejectedIE.class)).isEqualTo(
                new ReservationCheckoutRejectedIE("reservation-event-4", meis, MEI_CUSTOMER_ID, "TIME_TAKEN",
                        monday(20, 2)));
    }

    @Test
    @Tag("RES-11")
    @DisplayName("Mei's payment saves first while Aisyah's is being saved: Aisyah's save reloads, finds the time taken and is refunded.")
    void paymentThatLosesTheRaceIsRetriedAndRejected() {
        String aisyahs = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        String meis = bed.startStandardCheckout(MEI_CUSTOMER_ID);
        bed.time.set(REQUESTED_AT);
        PaymentHeldIE meiPaid = bed.paymentHeld(meis, REQUESTED_AT);
        bed.reservations.beforeNextSave(() -> bed.paymentHeld.handle(meiPaid));
        bed.publisher.clear();

        bed.paymentHeld.handle(bed.paymentHeld(aisyahs, REQUESTED_AT));

        assertThat(bed.reservations.single().booking().customerId()).isEqualTo(MEI_CUSTOMER_ID);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.published()).hasSize(2);
        assertThat(bed.publisher.published().get(0)).isInstanceOf(ReservationRequestedIE.class);
        assertThat(bed.publisher.published().get(1)).isEqualTo(new ReservationCheckoutRejectedIE(
                "reservation-event-4", aisyahs, AISYAH_CUSTOMER_ID, "TIME_TAKEN", REQUESTED_AT));
    }

    @Test
    @Tag("RES-11")
    @DisplayName("A save that conflicts twice is tried again and the reservation is Requested on the third try.")
    void conflictingSaveIsTriedAgain() {
        String checkoutId = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        bed.reservations.conflictOnNextSaves(2);
        bed.publisher.clear();

        bed.paymentHeld.handle(paid(checkoutId));

        assertThat(bed.reservations.single().status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(bed.publisher.single(ReservationRequestedIE.class).checkoutId()).isEqualTo(checkoutId);
    }

    @Test
    @Tag("RES-11")
    @DisplayName("After three conflicting saves the handler gives up; nothing is saved or published, and the event is redelivered.")
    void handlerGivesUpAfterThreeConflicts() {
        String checkoutId = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        bed.reservations.conflictOnNextSaves(3);
        bed.publisher.clear();

        assertThatThrownBy(() -> bed.paymentHeld.handle(paid(checkoutId)))
                .isInstanceOf(ScheduleChangedException.class);
        assertThat(bed.reservations.count()).isZero();
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Kemas Home raises its rate to RM30 while Aisyah pays: the price is no longer what she paid, so she is refunded.")
    void paymentForAPriceNoLongerOfferedIsRejected() {
        String checkoutId = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        ReservationTestBed.ListingEvent raised = bed.kemasListing();
        raised.rate = Money.myr(30);
        bed.listingTermsChanged.handle(raised.build(bed.nextInboundId()));
        bed.publisher.clear();

        bed.paymentHeld.handle(paid(checkoutId));

        assertThat(bed.reservations.count()).isZero();
        assertThat(bed.publisher.single(ReservationCheckoutRejectedIE.class).reason()).isEqualTo("RULE_FAILED");
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home stops being bookable while Aisyah pays: her payment is refunded as a failed rule.")
    void paymentForAProviderNoLongerBookableIsRejected() {
        String checkoutId = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        bed.bookabilityChanged.handle(bed.bookability(false, "NO_ACTIVE_HELPERS"));
        bed.publisher.clear();

        bed.paymentHeld.handle(paid(checkoutId));

        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationCheckoutRejectedIE.class)).isEqualTo(
                new ReservationCheckoutRejectedIE("reservation-event-2", checkoutId, AISYAH_CUSTOMER_ID,
                        "RULE_FAILED", REQUESTED_AT));
    }

    @Test
    @Tag("RES-7")
    @DisplayName("A redelivered payment confirmation changes nothing and publishes nothing.")
    void redeliveredPaymentIsIgnored() {
        String checkoutId = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        PaymentHeldIE event = paid(checkoutId);
        bed.paymentHeld.handle(event);
        Reservation first = bed.reservations.single();
        bed.publisher.clear();

        bed.paymentHeld.handle(event);

        assertThat(bed.reservations.single().id()).isEqualTo(first.id());
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
        assertThat(bed.processed.handled(event.eventId(), HandlePaymentHeldIE.HANDLER)).isTrue();
        assertThat(HandlePaymentHeldIE.HANDLER).isEqualTo("reservation.PaymentHeld");
    }

    @Test
    @Tag("RES-7")
    @DisplayName("A payment for a checkout Reservation never started, or for a customer it doesn't know, is not lost: it fails and is redelivered.")
    void paymentThatCannotBeMatchedFails() {
        assertThatThrownBy(() -> bed.paymentHeld.handle(paid("checkout-unknown")))
                .isInstanceOf(CheckoutNotFoundException.class);

        String checkoutId = bed.startStandardCheckout(AISYAH_CUSTOMER_ID);
        bed.customers.forget(AISYAH_CUSTOMER_ID);
        assertThatThrownBy(() -> bed.paymentHeld.handle(paid(checkoutId)))
                .isInstanceOf(CustomerNotFoundException.class);
        assertThat(bed.reservations.count()).isZero();
        assertThat(bed.publisher.published()).hasSize(1);
    }
}
