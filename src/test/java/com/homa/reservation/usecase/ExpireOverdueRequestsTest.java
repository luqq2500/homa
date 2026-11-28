package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.ACCEPTANCE_DEADLINE;
import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.KITCHEN_CLEANING;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.monday;
import static com.homa.reservation.usecase.ReservationTestBed.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ReservationExpiredIE;
import com.homa.ie.events.ReservationWithdrawnIE;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.ExpireOverdueRequestsRequest;
import com.homa.reservation.usecase.dto.ExpireOverdueRequestsResponse;
import com.homa.reservation.usecase.dto.ExpireRequestRequest;
import com.homa.reservation.usecase.dto.StartReservationCheckoutRequest;
import com.homa.reservation.usecase.dto.WithdrawReservationRequest;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import com.homa.reservation.usecase.port.in.RequestExpirer;
import com.homa.reservation.usecase.port.out.ReservationChangedException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ExpireOverdueRequestsTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation aisyahs = bed.requestStandard();

    /** Mei pays on Monday 20:30 for Friday 10:00, 4 hours, so her request is open until Tuesday 20:30. */
    private Reservation meisFridayRequest() {
        bed.time.set(monday(20, 29));
        String checkoutId = bed.startCheckout.start(new StartReservationCheckoutRequest(MEI_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, List.of(KITCHEN_CLEANING), friday(10, 0), 240, 2)).checkoutId();
        bed.time.set(monday(20, 30));
        bed.paymentHeld.handle(bed.paymentHeld(checkoutId, monday(20, 30)));
        return bed.reservations.findByParty(MEI_CUSTOMER_ID).get(0);
    }

    private ExpireOverdueRequestsResponse run() {
        return bed.expireOverdueRequests.expireOverdue(new ExpireOverdueRequestsRequest());
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("Kemas Home doesn't respond by Tuesday 20:00. Aisyah gets RM200 back.")
    void everyRequestPastItsDeadlineExpiresAndNoOtherDoes() {
        Reservation meis = meisFridayRequest();
        bed.publisher.clear();
        bed.time.set(ACCEPTANCE_DEADLINE);

        ExpireOverdueRequestsResponse first = run();

        assertThat(first.expiredReservationIds()).containsExactly(aisyahs.id().value());
        assertThat(first.skippedReservationIds()).isEmpty();
        assertThat(bed.reload(aisyahs).status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(bed.reload(meis).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(bed.publisher.single(ReservationExpiredIE.class).reservationId()).isEqualTo(aisyahs.id().value());

        bed.publisher.clear();
        bed.time.set(tuesday(20, 30));

        ExpireOverdueRequestsResponse second = run();

        assertThat(second.expiredReservationIds()).containsExactly(meis.id().value());
        assertThat(bed.reload(meis).status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(bed.reload(meis).expiredAt()).contains(tuesday(20, 30));
        assertThat(bed.publisher.single(ReservationExpiredIE.class).reservationId()).isEqualTo(meis.id().value());
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("A minute before Tuesday 20:00 nothing expires; at 20:00 the request does, and is refunded once.")
    void nothingExpiresBeforeADeadlineAndEveryRunAfterIsRepeatable() {
        bed.publisher.clear();
        bed.time.set(tuesday(19, 59));

        ExpireOverdueRequestsResponse early = run();

        assertThat(early.expiredReservationIds()).isEmpty();
        assertThat(early.skippedReservationIds()).isEmpty();
        assertThat(bed.reload(aisyahs).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(bed.publisher.published()).isEmpty();

        bed.time.set(ACCEPTANCE_DEADLINE);
        assertThat(run().expiredReservationIds()).containsExactly(aisyahs.id().value());
        bed.time.set(tuesday(20, 1));
        ExpireOverdueRequestsResponse repeated = run();

        assertThat(repeated.expiredReservationIds()).isEmpty();
        assertThat(repeated.skippedReservationIds()).isEmpty();
        assertThat(bed.publisher.single(ReservationExpiredIE.class).expiredAt()).isEqualTo(ACCEPTANCE_DEADLINE);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("A request Kemas Home accepted or Aisyah withdrew before Tuesday 20:00 is not expired.")
    void aRequestThatWasAcceptedOrWithdrawnIsNotExpired() {
        Reservation meis = meisFridayRequest();
        bed.time.set(monday(22, 0));
        bed.acceptReservation.accept(new AcceptReservationRequest(aisyahs.id().value(), KEMAS_HOME_PROVIDER_ID));
        bed.withdrawReservation.withdraw(new WithdrawReservationRequest(meis.id().value(), MEI_CUSTOMER_ID));
        bed.publisher.clear();
        bed.time.set(tuesday(21, 0));

        ExpireOverdueRequestsResponse response = run();

        assertThat(response.expiredReservationIds()).isEmpty();
        assertThat(response.skippedReservationIds()).isEmpty();
        assertThat(bed.reload(aisyahs).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.reload(meis).status()).isEqualTo(ReservationStatus.WITHDRAWN);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("When Aisyah's withdrawal commits while the run expires her request, the run skips it, expires the others, and only the withdrawal is published for it.")
    void aRequestWithdrawnWhileTheRunExpiresItIsSkipped() {
        Reservation meis = meisFridayRequest();
        bed.publisher.clear();
        bed.time.set(tuesday(20, 30));
        bed.reservations.beforeNextSave(() -> bed.withdrawReservation
                .withdraw(new WithdrawReservationRequest(aisyahs.id().value(), AISYAH_CUSTOMER_ID)));

        ExpireOverdueRequestsResponse response = run();

        assertThat(response.expiredReservationIds()).containsExactly(meis.id().value());
        assertThat(response.skippedReservationIds()).containsExactly(aisyahs.id().value());
        assertThat(bed.reload(aisyahs).status()).isEqualTo(ReservationStatus.WITHDRAWN);
        assertThat(bed.reload(meis).status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(bed.publisher.published()).hasSize(2);
        assertThat(bed.publisher.published().get(0)).isInstanceOf(ReservationWithdrawnIE.class);
        assertThat(((ReservationExpiredIE) bed.publisher.published().get(1)).reservationId())
                .isEqualTo(meis.id().value());
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("When another scheduler task expires the request while this run does, it is expired and published once.")
    void aRequestAnotherTaskExpiredFirstIsSkippedAndPublishedOnce() {
        bed.publisher.clear();
        bed.time.set(ACCEPTANCE_DEADLINE);
        bed.reservations.beforeNextSave(
                () -> bed.expireRequest.expire(new ExpireRequestRequest(aisyahs.id().value())));

        ExpireOverdueRequestsResponse response = run();

        assertThat(response.expiredReservationIds()).isEmpty();
        assertThat(response.skippedReservationIds()).containsExactly(aisyahs.id().value());
        assertThat(bed.reload(aisyahs).status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(bed.publisher.single(ReservationExpiredIE.class).reservationId()).isEqualTo(aisyahs.id().value());
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("A request whose expiry keeps conflicting is skipped, and the next run expires it.")
    void aRequestWhoseExpiryKeepsConflictingIsExpiredByTheNextRun() {
        bed.publisher.clear();
        bed.time.set(ACCEPTANCE_DEADLINE);
        bed.reservations.conflictOnNextSaves(3);

        ExpireOverdueRequestsResponse stuck = run();

        assertThat(stuck.expiredReservationIds()).isEmpty();
        assertThat(stuck.skippedReservationIds()).containsExactly(aisyahs.id().value());
        assertThat(bed.publisher.published()).isEmpty();

        bed.time.set(tuesday(20, 1));
        assertThat(run().expiredReservationIds()).containsExactly(aisyahs.id().value());
        assertThat(bed.publisher.single(ReservationExpiredIE.class).reservationId()).isEqualTo(aisyahs.id().value());
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("Each overdue request is expired through its own unit of work; one that changed under the run is skipped and the others still expire.")
    void eachOverdueRequestIsItsOwnUnitOfWork() {
        Reservation meis = meisFridayRequest();
        bed.time.set(tuesday(20, 30));
        List<String> asked = new java.util.ArrayList<>();
        RequestExpirer expirer = request -> {
            asked.add(request.reservationId());
            if (request.reservationId().equals(aisyahs.id().value())) {
                throw new ReservationChangedException(request.reservationId());
            }
            return bed.expireRequest.expire(request);
        };

        ExpireOverdueRequestsResponse response = new ExpireOverdueRequests(bed.reservations, expirer, bed.time)
                .expireOverdue(new ExpireOverdueRequestsRequest());

        assertThat(asked).containsExactlyInAnyOrder(aisyahs.id().value(), meis.id().value());
        assertThat(response.expiredReservationIds()).containsExactly(meis.id().value());
        assertThat(response.skippedReservationIds()).containsExactly(aisyahs.id().value());
        assertThat(bed.reload(aisyahs).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(bed.reload(meis).status()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("A schedule that keeps changing under an expiry skips that request for this run, and a failure of any other kind ends the run.")
    void aChangingScheduleIsSkippedButOtherFailuresEndTheRun() {
        bed.time.set(ACCEPTANCE_DEADLINE);
        RequestExpirer scheduleChanges = request -> {
            throw new ScheduleChangedException(KEMAS_HOME_PROVIDER_ID);
        };
        RequestExpirer breaks = request -> {
            throw new IllegalStateException("database down");
        };

        ExpireOverdueRequestsResponse skipped = new ExpireOverdueRequests(bed.reservations, scheduleChanges, bed.time)
                .expireOverdue(new ExpireOverdueRequestsRequest());

        assertThat(skipped.skippedReservationIds()).containsExactly(aisyahs.id().value());
        assertThatThrownBy(() -> new ExpireOverdueRequests(bed.reservations, breaks, bed.time)
                .expireOverdue(new ExpireOverdueRequestsRequest()))
                .isInstanceOf(IllegalStateException.class);
    }
}
