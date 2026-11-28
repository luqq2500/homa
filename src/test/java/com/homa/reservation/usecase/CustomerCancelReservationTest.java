package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.monday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.homa.reservation.usecase.ReservationTestBed.AZMAN_PERSON_ID;

import com.homa.ie.events.ReservationCancelledIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.JobAlreadyEndedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.CustomerCancelReservationRequest;
import com.homa.reservation.usecase.dto.CustomerCancelReservationResponse;
import com.homa.reservation.usecase.dto.ProviderCancelReservationRequest;
import com.homa.reservation.usecase.dto.WithdrawReservationRequest;
import com.homa.reservation.usecase.dto.WithdrawReservationResponse;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerCancelReservationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation reserved = bed.reserveStandard();

    private CustomerCancelReservationResponse cancel(String actorId) {
        return bed.customerCancel.cancel(new CustomerCancelReservationRequest(reserved.id().value(), actorId));
    }

    private void assertStillReserved() {
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.reload(reserved).cancellation()).isEmpty();
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    private ReservationCancelledIE cancelledEvent(String eventId, String cause, Instant at, String fee, String refund,
            String payout) {
        return new ReservationCancelledIE(eventId, reserved.id().value(), AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID,
                cause, at, REQUESTED_AT, saturday(9, 45), sunday(14, 0), "settlement-" + reserved.id().value(),
                Money.myr(fee), Money.myr(refund), Money.myr(0), Money.myr(0), Money.myr(payout), at);
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("Aisyah cancels on Friday 22:00, pays RM45 and gets RM155 back.")
    void customerPaysTheBaseCancellationFeeAfterTheCutOffAndBothCalendarsOpen() {
        bed.publisher.clear();
        bed.time.set(friday(22, 0));

        CustomerCancelReservationResponse cancelled = cancel(AISYAH_CUSTOMER_ID);

        assertThat(cancelled).isEqualTo(new CustomerCancelReservationResponse(reserved.id().value(), "CANCELLED",
                "CUSTOMER_CANCELLED", friday(22, 0), Money.myr(45), Money.myr(155)));
        Reservation saved = bed.reload(reserved);
        assertThat(saved.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(saved.cancellation().orElseThrow().at()).isEqualTo(friday(22, 0));
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationCancelledIE.class)).isEqualTo(
                cancelledEvent("reservation-event-4", "CUSTOMER_CANCELLED", friday(22, 0), "45", "155", "45"));
    }

    @Test
    @Tag("CAN-1")
    @DisplayName("The Base Cancellation Fee is RM45, not RM50.")
    void cancellationFeeIsCalculatedAfterDiscount() {
        bed.time.set(friday(22, 0));

        CustomerCancelReservationResponse cancelled = cancel(AISYAH_CUSTOMER_ID);

        assertThat(cancelled.customerFee()).isEqualTo(Money.myr(45)).isNotEqualTo(Money.myr(50));
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("The time Aisyah cancelled opens at once: Mei's payment for it, a minute later, makes her request.")
    void aCancelledReservationLeavesTheTimeToTheNextPayment() {
        bed.time.set(friday(22, 0));
        cancel(AISYAH_CUSTOMER_ID);
        bed.time.set(friday(22, 1));
        String meiCheckout = bed.startCheckout.start(ReservationTestBed.standardCheckout(MEI_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID)).checkoutId();
        bed.publisher.clear();

        bed.paymentHeld.handle(bed.paymentHeld(meiCheckout, friday(22, 2)));

        assertThat(bed.publisher.published()).hasSize(1);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
    }

    @Test
    @Tag("CCX-1")
    @DisplayName("Reserved Monday 22:00, Aisyah cancels at 23:30 for free.")
    void customerCancelsForFreeWithinTheGracePeriod() {
        bed.publisher.clear();
        bed.time.set(monday(23, 30));

        CustomerCancelReservationResponse cancelled = cancel(AISYAH_CUSTOMER_ID);

        assertThat(cancelled).isEqualTo(new CustomerCancelReservationResponse(reserved.id().value(), "CANCELLED",
                "CUSTOMER_CANCELLED", monday(23, 30), Money.myr(0), Money.myr(200)));
        assertThat(bed.publisher.single(ReservationCancelledIE.class)).isEqualTo(
                cancelledEvent("reservation-event-4", "CUSTOMER_CANCELLED", monday(23, 30), "0", "200", "0"));
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Kemas Home accepts at 21:02:10 and Aisyah withdraws at 21:02:12. The reservation is Reserved, and she can cancel for free within her Grace Period (CCX-1).")
    void customerWhoseWithdrawalLostToAnAcceptanceCancelsForFreeInTheGracePeriod() {
        ReservationTestBed other = ReservationTestBed.withKemasHomeAndAisyah();
        Reservation requested = other.requestStandard();
        other.time.set(monday(21, 2).plusSeconds(10));
        other.acceptReservation.accept(new AcceptReservationRequest(requested.id().value(), AZMAN_PERSON_ID));
        other.time.set(monday(21, 2).plusSeconds(12));

        WithdrawReservationResponse withdrawn = other.withdrawReservation
                .withdraw(new WithdrawReservationRequest(requested.id().value(), AISYAH_CUSTOMER_ID));
        other.time.set(monday(21, 2).plusSeconds(30));
        other.publisher.clear();
        CustomerCancelReservationResponse cancelled = other.customerCancel
                .cancel(new CustomerCancelReservationRequest(requested.id().value(), AISYAH_CUSTOMER_ID));

        assertThat(withdrawn.outcome()).isEqualTo("ALREADY_ACCEPTED");
        assertThat(cancelled.cause()).isEqualTo("CUSTOMER_CANCELLED");
        assertThat(cancelled.customerFee()).isEqualTo(Money.myr(0));
        assertThat(cancelled.customerRefund()).isEqualTo(Money.myr(200));
        assertThat(other.publisher.single(ReservationCancelledIE.class).customerFee()).isEqualTo(Money.myr(0));
    }

    @Test
    @Tag("CCX-4")
    @DisplayName("Standard: free until Friday 21:45.")
    void standardCutOffIsDecidedByTheReservationAtTheMomentOfTheCall() {
        ReservationTestBed late = ReservationTestBed.withKemasHomeAndAisyah();
        Reservation lateReservation = late.reserveStandard();
        bed.time.set(friday(21, 44));
        late.time.set(friday(21, 45));

        assertThat(cancel(AISYAH_CUSTOMER_ID).customerFee()).isEqualTo(Money.myr(0));
        assertThat(late.customerCancel.cancel(new CustomerCancelReservationRequest(lateReservation.id().value(),
                AISYAH_CUSTOMER_ID)).customerFee()).isEqualTo(Money.myr(45));
    }

    @Test
    @Tag("LMC-1")
    @DisplayName("Aisyah cancels at 9:50: she pays 45 + 0 + 20 = RM65 and gets RM135 back.")
    void customerCancellingInTheCheckInPeriodPaysBasePlusTravel() {
        bed.publisher.clear();
        bed.time.set(saturday(9, 50));

        CustomerCancelReservationResponse cancelled = cancel(AISYAH_CUSTOMER_ID);

        assertThat(cancelled).isEqualTo(new CustomerCancelReservationResponse(reserved.id().value(), "CANCELLED",
                "CUSTOMER_LAST_MINUTE", saturday(9, 50), Money.myr(65), Money.myr(135)));
        assertThat(bed.publisher.single(ReservationCancelledIE.class)).isEqualTo(
                cancelledEvent("reservation-event-4", "CUSTOMER_LAST_MINUTE", saturday(9, 50), "65", "135", "65"));
    }

    @Test
    @Tag("LMC-2")
    @DisplayName("Aisyah stops the job at 11:20: 30 minutes are charged (RM22.50), so she pays RM87.50 and gets RM112.50 back.")
    void customerStoppingTheJobPaysTheStartedQuarterHours() {
        bed.publisher.clear();
        bed.time.set(saturday(11, 20));

        CustomerCancelReservationResponse cancelled = cancel(AISYAH_CUSTOMER_ID);

        assertThat(cancelled.customerFee()).isEqualTo(Money.myr("87.50"));
        assertThat(cancelled.customerRefund()).isEqualTo(Money.myr("112.50"));
        assertThat(bed.publisher.single(ReservationCancelledIE.class)).isEqualTo(
                cancelledEvent("reservation-event-4", "CUSTOMER_LAST_MINUTE", saturday(11, 20), "87.50", "112.50",
                        "87.50"));
    }

    @Test
    @Tag("REQ-1")
    @DisplayName("A Requested reservation is withdrawn, not cancelled: Aisyah can't cancel it, and nothing changes.")
    void aRequestedReservationCannotBeCancelled() {
        ReservationTestBed other = ReservationTestBed.withKemasHomeAndAisyah();
        Reservation requested = other.requestStandard();
        other.publisher.clear();
        other.time.set(monday(21, 0));

        assertThatThrownBy(() -> other.customerCancel
                .cancel(new CustomerCancelReservationRequest(requested.id().value(), AISYAH_CUSTOMER_ID)))
                .isInstanceOf(ReservationStatusConflictException.class);

        assertThat(other.reload(requested).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(other.publisher.published()).isEmpty();
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("Only Aisyah cancels her reservation: neither Kemas Home nor Mei can, and an unknown reservation is refused.")
    void onlyTheReservationsCustomerCancels() {
        bed.publisher.clear();
        bed.time.set(friday(22, 0));

        assertThatThrownBy(() -> cancel(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> cancel(MEI_CUSTOMER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> bed.customerCancel
                .cancel(new CustomerCancelReservationRequest("reservation-unknown", AISYAH_CUSTOMER_ID)))
                .isInstanceOf(ReservationNotFoundException.class);

        assertStillReserved();
    }

    @Test
    @Tag("LMC-1")
    @DisplayName("The job ended at 14:00, so Aisyah can no longer cancel it, and nothing changes.")
    void aJobThatEndedCannotBeCancelled() {
        bed.publisher.clear();
        bed.time.set(JOB_END);

        assertThatThrownBy(() -> cancel(AISYAH_CUSTOMER_ID)).isInstanceOf(JobAlreadyEndedException.class);

        assertStillReserved();
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("A cancelled reservation is not cancelled again, and nothing more is published.")
    void aCancelledReservationIsNotCancelledTwice() {
        bed.time.set(friday(22, 0));
        cancel(AISYAH_CUSTOMER_ID);
        bed.publisher.clear();
        bed.time.set(friday(22, 5));

        assertThatThrownBy(() -> cancel(AISYAH_CUSTOMER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(reserved).cancellation().orElseThrow().at()).isEqualTo(friday(22, 0));
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("PCX-2")
    @DisplayName("Kemas Home's cancellation commits while Aisyah's is being saved: hers reloads, finds the reservation Cancelled and is refused, so only Kemas Home's settlement is published.")
    void customerCancellationThatLosesTheRaceToTheProvidersIsRefused() {
        bed.publisher.clear();
        bed.time.set(friday(20, 0));
        bed.reservations.beforeNextSave(() -> bed.providerCancel
                .cancel(new ProviderCancelReservationRequest(reserved.id().value(), AZMAN_PERSON_ID)));

        assertThatThrownBy(() -> cancel(AISYAH_CUSTOMER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(reserved).cancellation().orElseThrow().cause().name()).isEqualTo("PROVIDER_CANCELLED");
        assertThat(bed.publisher.single(ReservationCancelledIE.class).cause()).isEqualTo("PROVIDER_CANCELLED");
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("When another save moved a schedule on meanwhile, the cancellation reloads and is saved on the next try; after three conflicts it fails and nothing changes.")
    void cancellationIsRetriedAfterAConflictAndGivesUpAfterThree() {
        bed.publisher.clear();
        bed.time.set(friday(22, 0));
        bed.reservations.conflictOnNextSaves(3);

        assertThatThrownBy(() -> cancel(AISYAH_CUSTOMER_ID)).isInstanceOf(ScheduleChangedException.class);
        assertStillReserved();

        bed.reservations.conflictOnNextSaves(2);
        cancel(AISYAH_CUSTOMER_ID);

        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(bed.publisher.single(ReservationCancelledIE.class).customerFee()).isEqualTo(Money.myr(45));
    }
}
