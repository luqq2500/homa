package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.ACCEPTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.sunday;
import static com.homa.reservation.usecase.ReservationTestBed.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.ie.events.ReservationWithdrawnIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.AcceptanceDeadlinePassedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.AcceptReservationResponse;
import com.homa.reservation.usecase.dto.DeclineReservationRequest;
import com.homa.reservation.usecase.dto.ExpireRequestRequest;
import com.homa.reservation.usecase.dto.WithdrawReservationRequest;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderAcceptReservationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation requested = bed.requestStandard();

    private AcceptReservationResponse accept(String actorId) {
        return bed.acceptReservation.accept(new AcceptReservationRequest(requested.id().value(), actorId));
    }

    @Test
    @Tag("ACC-2")
    @DisplayName("Kemas Home accepts on Monday 22:00, and Aisyah's booking is Reserved.")
    void providerAcceptsAndTheReservationIsReserved() {
        bed.publisher.clear();
        bed.time.set(ACCEPTED_AT);

        AcceptReservationResponse accepted = accept(KEMAS_HOME_PROVIDER_ID);

        String id = requested.id().value();
        assertThat(accepted).isEqualTo(new AcceptReservationResponse(id, "RESERVED", ACCEPTED_AT));
        Reservation saved = bed.reload(requested);
        assertThat(saved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(saved.reservedAt()).contains(ACCEPTED_AT);
        assertThat(bed.publisher.single(ReservationReservedIE.class)).isEqualTo(new ReservationReservedIE(
                "reservation-event-3", id, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, saturday(9, 45), JOB_START,
                JOB_END, Money.myr(200), sunday(14, 0), REQUESTED_AT, ACCEPTED_AT));
    }

    @Test
    @Tag("ACC-2")
    @DisplayName("Kemas Home doesn't respond by Tuesday 20:00, so it can no longer accept, and nothing changes.")
    void requestCannotBeAcceptedAfterItsDeadline() {
        bed.publisher.clear();
        bed.time.set(HomaFixtures.ACCEPTANCE_DEADLINE);

        assertThatThrownBy(() -> accept(KEMAS_HOME_PROVIDER_ID))
                .isInstanceOf(AcceptanceDeadlinePassedException.class);
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("A Monday 20:00 request has until Tuesday 20:00.")
    void aRequestHasTwentyFourHoursToBeAccepted() {
        bed.time.set(tuesday(19, 59));

        assertThat(accept(KEMAS_HOME_PROVIDER_ID).status()).isEqualTo("RESERVED");
        assertThat(bed.reload(requested).reservedAt()).contains(tuesday(19, 59));
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("A Saturday 7:00 request for 10:00 has until 8:45.")
    void aLateRequestHasUntilAnHourBeforeTheCheckInPeriod() {
        ReservationTestBed lateBed = ReservationTestBed.withKemasHomeAndAisyah();
        lateBed.time.set(saturday(6, 58));
        String checkoutId = lateBed.startCheckout
                .start(ReservationTestBed.standardCheckout(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID)).checkoutId();
        lateBed.time.set(saturday(7, 0));
        lateBed.paymentHeld.handle(lateBed.paymentHeld(checkoutId, saturday(7, 0)));
        String id = lateBed.reservations.single().id().value();
        AcceptReservationRequest accepting = new AcceptReservationRequest(id, KEMAS_HOME_PROVIDER_ID);

        lateBed.time.set(saturday(8, 45));
        assertThatThrownBy(() -> lateBed.acceptReservation.accept(accepting))
                .isInstanceOf(AcceptanceDeadlinePassedException.class);
        lateBed.time.set(saturday(8, 44));
        lateBed.acceptReservation.accept(accepting);

        assertThat(lateBed.reload(id).status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @Tag("ACC-2")
    @DisplayName("Only Kemas Home accepts its requests: neither Aisyah nor another provider can.")
    void onlyTheReservationsProviderAccepts() {
        bed.publisher.clear();
        bed.time.set(ACCEPTED_AT);

        assertThatThrownBy(() -> accept(AISYAH_CUSTOMER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> accept("provider-another")).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> bed.acceptReservation.accept(
                new AcceptReservationRequest("reservation-unknown", KEMAS_HOME_PROVIDER_ID)))
                .isInstanceOf(ReservationNotFoundException.class);
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Had Aisyah's withdrawal committed first, Kemas Home's acceptance would be refused: it reloads, finds the reservation Withdrawn, and only the withdrawal is published.")
    void acceptanceThatLosesTheRaceToAWithdrawalIsRefused() {
        bed.publisher.clear();
        bed.time.set(monday21Oh2());
        bed.reservations.beforeNextSave(() -> bed.withdrawReservation
                .withdraw(new WithdrawReservationRequest(requested.id().value(), AISYAH_CUSTOMER_ID)));

        assertThatThrownBy(() -> accept(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        Reservation saved = bed.reload(requested);
        assertThat(saved.status()).isEqualTo(ReservationStatus.WITHDRAWN);
        assertThat(saved.reservedAt()).isEmpty();
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.publisher.published()).hasSize(1).first().isInstanceOf(ReservationWithdrawnIE.class);
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Aisyah's withdrawal recorded first at 21:02:10 beats Kemas Home's acceptance at 21:02:12, which is refused.")
    void acceptanceRecordedTwoSecondsAfterAWithdrawalIsRefused() {
        bed.time.set(monday21Oh2().plusSeconds(10));
        bed.withdrawReservation.withdraw(new WithdrawReservationRequest(requested.id().value(), AISYAH_CUSTOMER_ID));
        bed.publisher.clear();
        bed.time.set(monday21Oh2().plusSeconds(12));

        assertThatThrownBy(() -> accept(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.WITHDRAWN);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("A request Kemas Home declined, or that expired at Tuesday 20:00, can't be accepted afterwards.")
    void aDeclinedOrExpiredRequestCannotBeAccepted() {
        ReservationTestBed other = ReservationTestBed.withKemasHomeAndAisyah();
        Reservation declined = other.requestStandard();
        other.time.set(ACCEPTED_AT);
        other.declineReservation.decline(new DeclineReservationRequest(declined.id().value(), KEMAS_HOME_PROVIDER_ID));
        other.publisher.clear();

        assertThatThrownBy(() -> other.acceptReservation
                .accept(new AcceptReservationRequest(declined.id().value(), KEMAS_HOME_PROVIDER_ID)))
                .isInstanceOf(ReservationStatusConflictException.class);

        bed.time.set(HomaFixtures.ACCEPTANCE_DEADLINE);
        bed.expireRequest.expire(new ExpireRequestRequest(requested.id().value()));
        bed.time.set(ReservationTestBed.tuesday(20, 1));
        assertThatThrownBy(() -> accept(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(other.reload(declined).status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.EXPIRED);
    }

    private static java.time.Instant monday21Oh2() {
        return ReservationTestBed.monday(21, 2);
    }
}
