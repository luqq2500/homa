package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.ACCEPTANCE_DEADLINE;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.ACCEPTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.monday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ReservationExpiredIE;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.AcceptanceDeadlineNotReachedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.ExpireRequestRequest;
import com.homa.reservation.usecase.dto.ExpireRequestResponse;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ExpireRequestTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation requested = bed.requestStandard();

    private ExpireRequestResponse expire() {
        return bed.expireRequest.expire(new ExpireRequestRequest(requested.id().value()));
    }

    private void assertStillRequested() {
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("Kemas Home doesn't respond by Tuesday 20:00. Aisyah gets RM200 back.")
    void requestExpiresAtItsDeadlineAndBothCalendarsOpen() {
        bed.publisher.clear();
        bed.time.set(ACCEPTANCE_DEADLINE);

        ExpireRequestResponse expired = expire();

        String id = requested.id().value();
        assertThat(expired).isEqualTo(new ExpireRequestResponse(id, "EXPIRED", ACCEPTANCE_DEADLINE));
        Reservation saved = bed.reload(requested);
        assertThat(saved.status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(saved.expiredAt()).contains(ACCEPTANCE_DEADLINE);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationExpiredIE.class)).isEqualTo(new ReservationExpiredIE(
                "reservation-event-3", id, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, ACCEPTANCE_DEADLINE,
                REQUESTED_AT, ACCEPTANCE_DEADLINE));
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("A minute before Tuesday 20:00 the request is still open, so it can't expire; a minute after it can.")
    void requestCannotExpireBeforeItsDeadline() {
        bed.publisher.clear();
        bed.time.set(tuesday(19, 59));

        assertThatThrownBy(this::expire).isInstanceOf(AcceptanceDeadlineNotReachedException.class);
        assertStillRequested();

        bed.time.set(tuesday(20, 1));
        assertThat(expire().expiredAt()).isEqualTo(tuesday(20, 1));
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("A Saturday 7:00 request for 10:00 has until 8:45, and then it expires.")
    void aLateRequestExpiresAnHourBeforeTheCheckInPeriod() {
        ReservationTestBed lateBed = ReservationTestBed.withKemasHomeAndAisyah();
        lateBed.time.set(saturday(6, 58));
        String checkoutId = lateBed.startCheckout
                .start(ReservationTestBed.standardCheckout(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID)).checkoutId();
        lateBed.time.set(saturday(7, 0));
        lateBed.paymentHeld.handle(lateBed.paymentHeld(checkoutId, saturday(7, 0)));
        ExpireRequestRequest expiring = new ExpireRequestRequest(lateBed.reservations.single().id().value());

        lateBed.time.set(saturday(8, 44));
        assertThatThrownBy(() -> lateBed.expireRequest.expire(expiring))
                .isInstanceOf(AcceptanceDeadlineNotReachedException.class);
        lateBed.time.set(saturday(8, 45));
        lateBed.expireRequest.expire(expiring);

        assertThat(lateBed.reservations.single().status()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("A request Kemas Home accepted in time doesn't expire at Tuesday 20:00, and nothing changes.")
    void anAcceptedRequestDoesNotExpire() {
        bed.time.set(ACCEPTED_AT);
        bed.acceptReservation.accept(new AcceptReservationRequest(requested.id().value(), KEMAS_HOME_PROVIDER_ID));
        bed.publisher.clear();
        bed.time.set(ACCEPTANCE_DEADLINE);

        assertThatThrownBy(this::expire).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("A request already expired expires no second time, and nothing more is published.")
    void anExpiredRequestIsNotExpiredTwice() {
        bed.time.set(ACCEPTANCE_DEADLINE);
        expire();
        bed.publisher.clear();
        bed.time.set(tuesday(20, 5));

        assertThatThrownBy(this::expire).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(requested).expiredAt()).contains(ACCEPTANCE_DEADLINE);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("When another save moved a schedule on meanwhile, the expiry reloads and is saved on the next try; after three conflicts it fails and nothing changes.")
    void expiryIsRetriedAfterAConflictAndGivesUpAfterThree() {
        bed.publisher.clear();
        bed.time.set(ACCEPTANCE_DEADLINE);
        bed.reservations.conflictOnNextSaves(3);

        assertThatThrownBy(this::expire).isInstanceOf(ScheduleChangedException.class);
        assertStillRequested();

        bed.reservations.conflictOnNextSaves(2);
        expire();

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(bed.publisher.single(ReservationExpiredIE.class).reservationId()).isEqualTo(requested.id().value());
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("An unknown reservation can't expire.")
    void anUnknownReservationCannotExpire() {
        bed.time.set(monday(21, 0));

        assertThatThrownBy(() -> bed.expireRequest.expire(new ExpireRequestRequest("reservation-unknown")))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
