package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.KITCHEN_CLEANING;
import static com.homa.reservation.usecase.ReservationTestBed.LAUNDRY_FOLDING;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.monday;
import static com.homa.reservation.usecase.ReservationTestBed.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.homa.reservation.usecase.ReservationTestBed.AZMAN_PERSON_ID;

import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.events.ReservationRequestedIE;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.ie.events.ReservationWithdrawnIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.DeclineReservationRequest;
import com.homa.reservation.usecase.dto.StartReservationCheckoutRequest;
import com.homa.reservation.usecase.dto.WithdrawReservationRequest;
import com.homa.reservation.usecase.dto.WithdrawReservationResponse;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CustomerWithdrawReservationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation requested = bed.requestStandard();

    private WithdrawReservationResponse withdraw(String actorId) {
        return bed.withdrawReservation.withdraw(new WithdrawReservationRequest(requested.id().value(), actorId));
    }

    private void assertStillRequested() {
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    /** The reservation the provider accepted first and the customer then tried to withdraw. */
    private void assertAcceptedFirst(WithdrawReservationResponse response) {
        assertThat(response).isEqualTo(new WithdrawReservationResponse(requested.id().value(), "ALREADY_ACCEPTED",
                "RESERVED", null));
        Reservation saved = bed.reload(requested);
        assertThat(saved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(saved.withdrawnAt()).isEmpty();
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).hasSize(1).first().isInstanceOf(ReservationReservedIE.class);
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("Aisyah withdraws on Monday 21:00 and gets RM200 back.")
    void customerWithdrawsARequestForFreeAndBothCalendarsOpen() {
        bed.publisher.clear();
        bed.time.set(monday(21, 0));

        WithdrawReservationResponse withdrawn = withdraw(AISYAH_CUSTOMER_ID);

        String id = requested.id().value();
        assertThat(withdrawn).isEqualTo(new WithdrawReservationResponse(id, "WITHDRAWN", "WITHDRAWN", monday(21, 0)));
        Reservation saved = bed.reload(requested);
        assertThat(saved.status()).isEqualTo(ReservationStatus.WITHDRAWN);
        assertThat(saved.withdrawnAt()).contains(monday(21, 0));
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationWithdrawnIE.class)).isEqualTo(new ReservationWithdrawnIE(
                "reservation-event-3", id, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, monday(21, 0), REQUESTED_AT,
                monday(21, 0)));
    }

    @Test
    @Tag("RES-9")
    @DisplayName("A withdrawal frees the time: Mei's payment for it, a minute later, makes her request.")
    void aWithdrawnReservationLeavesTheTimeToTheNextPayment() {
        bed.time.set(monday(21, 0));
        withdraw(AISYAH_CUSTOMER_ID);
        bed.time.set(monday(21, 1));
        String meiCheckout = bed.startCheckout.start(ReservationTestBed.standardCheckout(MEI_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID)).checkoutId();
        bed.publisher.clear();

        bed.paymentHeld.handle(bed.paymentHeld(meiCheckout, monday(21, 2)));

        assertThat(bed.publisher.published()).hasSize(1).first().isInstanceOf(ReservationRequestedIE.class);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
    }

    @Test
    @Tag("REQ-1")
    @DisplayName("Aisyah wants 3 hours instead of 4. She withdraws and requests 3 hours (RM155) as a new request, with a new deadline. The slot isn't held in between (P1).")
    void changeIsAWithdrawalAndANewRequestWithANewDeadline() {
        bed.time.set(monday(21, 0));
        withdraw(AISYAH_CUSTOMER_ID);

        String checkoutId = bed.startCheckout.start(new StartReservationCheckoutRequest(AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, List.of(KITCHEN_CLEANING, LAUNDRY_FOLDING), JOB_START, 180, 2)).checkoutId();
        bed.publisher.clear();
        bed.paymentHeld.handle(new PaymentHeldIE(bed.nextInboundId(), checkoutId, Money.myr(155), monday(21, 0),
                monday(21, 0)));

        ReservationRequestedIE again = bed.publisher.single(ReservationRequestedIE.class);
        assertThat(again.reservationId()).isNotEqualTo(requested.id().value());
        assertThat(again.total()).isEqualTo(Money.myr(155));
        assertThat(again.acceptanceDeadline()).isEqualTo(tuesday(21, 0));
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.WITHDRAWN);
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Kemas Home accepts at 21:02:10 and Aisyah withdraws at 21:02:12. The reservation is Reserved, and she can cancel for free within her Grace Period (CCX-1).")
    void withdrawalRecordedTwoSecondsAfterAnAcceptanceIsToldItWasAccepted() {
        bed.time.set(monday(21, 2).plusSeconds(10));
        bed.acceptReservation.accept(new AcceptReservationRequest(requested.id().value(), AZMAN_PERSON_ID));
        bed.time.set(monday(21, 2).plusSeconds(12));

        WithdrawReservationResponse response = withdraw(AISYAH_CUSTOMER_ID);

        bed.publisher.clear();
        assertThat(response.outcome()).isEqualTo(WithdrawReservationResponse.ALREADY_ACCEPTED);
        assertThat(response.status()).isEqualTo("RESERVED");
        assertThat(response.withdrawnAt()).isNull();
        Reservation saved = bed.reload(requested);
        assertThat(saved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(saved.reservedAt()).contains(monday(21, 2).plusSeconds(10));
        assertThat(saved.withdrawnAt()).isEmpty();
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Kemas Home's acceptance commits while Aisyah's withdrawal is being saved: the withdrawal reloads, finds the reservation Reserved, and nothing is withdrawn.")
    void withdrawalThatLosesTheRaceToAnAcceptanceReloadsAndOffersCancellation() {
        bed.publisher.clear();
        bed.time.set(monday(21, 2).plusSeconds(10));
        bed.reservations.beforeNextSave(() -> bed.acceptReservation
                .accept(new AcceptReservationRequest(requested.id().value(), AZMAN_PERSON_ID)));

        WithdrawReservationResponse response = withdraw(AISYAH_CUSTOMER_ID);

        assertAcceptedFirst(response);
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("When the acceptance and the withdrawal are both saved at once, the second save is refused and only one of them is recorded.")
    void aSecondSaveOfTheSameVersionIsRefused() {
        Reservation acceptingCopy = bed.reservations.findById(requested.id()).orElseThrow();
        Reservation withdrawingCopy = bed.reservations.findById(requested.id()).orElseThrow();
        acceptingCopy.accept(monday(21, 2));
        withdrawingCopy.withdraw(monday(21, 2));

        bed.reservations.save(acceptingCopy);

        assertThatThrownBy(() -> bed.reservations.save(withdrawingCopy))
                .isInstanceOf(com.homa.reservation.usecase.port.out.ReservationChangedException.class);
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.reload(requested).version()).isEqualTo(requested.version() + 1);
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("When another save moved a schedule on meanwhile, the withdrawal reloads and is saved on the next try.")
    void withdrawalIsRetriedAfterAScheduleConflict() {
        bed.publisher.clear();
        bed.time.set(monday(21, 0));
        bed.reservations.conflictOnNextSaves(2);

        WithdrawReservationResponse response = withdraw(AISYAH_CUSTOMER_ID);

        assertThat(response.outcome()).isEqualTo("WITHDRAWN");
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.WITHDRAWN);
        assertThat(bed.publisher.single(ReservationWithdrawnIE.class).withdrawnAt()).isEqualTo(monday(21, 0));
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("After three conflicting saves the withdrawal fails as a whole and nothing changes.")
    void withdrawalGivesUpAfterThreeConflicts() {
        bed.publisher.clear();
        bed.time.set(monday(21, 0));
        bed.reservations.conflictOnNextSaves(3);

        assertThatThrownBy(() -> withdraw(AISYAH_CUSTOMER_ID)).isInstanceOf(ScheduleChangedException.class);

        assertStillRequested();
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("Only Aisyah withdraws her requests: neither Kemas Home nor Mei can, and an unknown reservation is refused.")
    void onlyTheReservationsCustomerWithdraws() {
        bed.publisher.clear();
        bed.time.set(monday(21, 0));

        assertThatThrownBy(() -> withdraw(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> withdraw(MEI_CUSTOMER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> bed.withdrawReservation.withdraw(
                new WithdrawReservationRequest("reservation-unknown", AISYAH_CUSTOMER_ID)))
                .isInstanceOf(ReservationNotFoundException.class);

        assertStillRequested();
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("A reservation already withdrawn can't be withdrawn again, and nothing more is published.")
    void aWithdrawnReservationIsNotWithdrawnTwice() {
        bed.time.set(monday(21, 0));
        withdraw(AISYAH_CUSTOMER_ID);
        bed.publisher.clear();
        bed.time.set(monday(21, 5));

        assertThatThrownBy(() -> withdraw(AISYAH_CUSTOMER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(requested).withdrawnAt()).contains(monday(21, 0));
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("A request Kemas Home declined first is declined, so Aisyah's withdrawal is refused and nothing more is published.")
    void aDeclinedReservationCannotBeWithdrawn() {
        bed.time.set(monday(21, 0));
        bed.declineReservation.decline(new DeclineReservationRequest(requested.id().value(), AZMAN_PERSON_ID));
        bed.publisher.clear();
        bed.time.set(monday(21, 5));

        assertThatThrownBy(() -> withdraw(AISYAH_CUSTOMER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("A request still open a minute before its deadline can be withdrawn.")
    void aRequestCanBeWithdrawnRightUpToItsDeadline() {
        bed.publisher.clear();
        bed.time.set(tuesday(20, 0).minus(Duration.ofMinutes(1)));

        assertThat(withdraw(AISYAH_CUSTOMER_ID).outcome()).isEqualTo("WITHDRAWN");
    }
}
