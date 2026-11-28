package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.ACCEPTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.monday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ReservationDeclinedIE;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.ie.events.ReservationWithdrawnIE;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.DeclineReservationRequest;
import com.homa.reservation.usecase.dto.DeclineReservationResponse;
import com.homa.reservation.usecase.dto.WithdrawReservationRequest;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderDeclineReservationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation requested = bed.requestStandard();

    private DeclineReservationResponse decline(String actorId) {
        return bed.declineReservation.decline(new DeclineReservationRequest(requested.id().value(), actorId));
    }

    private void assertStillRequested() {
        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("Kemas Home declines because two helpers are on leave. Aisyah gets RM200 back.")
    void providerDeclinesARequestAndBothCalendarsOpen() {
        bed.publisher.clear();
        bed.time.set(monday(21, 0));

        DeclineReservationResponse declined = decline(KEMAS_HOME_PROVIDER_ID);

        String id = requested.id().value();
        assertThat(declined).isEqualTo(new DeclineReservationResponse(id, "DECLINED", monday(21, 0)));
        Reservation saved = bed.reload(requested);
        assertThat(saved.status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(saved.declinedAt()).contains(monday(21, 0));
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationDeclinedIE.class)).isEqualTo(new ReservationDeclinedIE(
                "reservation-event-3", id, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, monday(21, 0), REQUESTED_AT,
                monday(21, 0)));
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("Only Kemas Home declines its requests: neither Aisyah nor another provider can, and an unknown reservation is refused.")
    void onlyTheReservationsProviderDeclines() {
        bed.publisher.clear();
        bed.time.set(monday(21, 0));

        assertThatThrownBy(() -> decline(AISYAH_CUSTOMER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> decline("provider-another")).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> bed.declineReservation.decline(
                new DeclineReservationRequest("reservation-unknown", KEMAS_HOME_PROVIDER_ID)))
                .isInstanceOf(ReservationNotFoundException.class);

        assertStillRequested();
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("A reservation Kemas Home already accepted can't be declined, and nothing changes.")
    void anAcceptedReservationCannotBeDeclined() {
        bed.time.set(ACCEPTED_AT);
        bed.acceptReservation.accept(new AcceptReservationRequest(requested.id().value(), KEMAS_HOME_PROVIDER_ID));
        bed.publisher.clear();

        assertThatThrownBy(() -> decline(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Aisyah's withdrawal commits while Kemas Home's decline is being saved: the decline reloads, finds the reservation Withdrawn, is refused, and only the withdrawal is published.")
    void declineThatLosesTheRaceToAWithdrawalIsRefused() {
        bed.publisher.clear();
        bed.time.set(monday(21, 0));
        bed.reservations.beforeNextSave(() -> bed.withdrawReservation
                .withdraw(new WithdrawReservationRequest(requested.id().value(), AISYAH_CUSTOMER_ID)));

        assertThatThrownBy(() -> decline(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.WITHDRAWN);
        assertThat(bed.publisher.published()).hasSize(1).first().isInstanceOf(ReservationWithdrawnIE.class);
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Kemas Home's acceptance commits while its decline is being saved: the decline reloads, finds the reservation Reserved and is refused.")
    void declineThatLosesTheRaceToAnAcceptanceIsRefused() {
        bed.publisher.clear();
        bed.time.set(ACCEPTED_AT);
        bed.reservations.beforeNextSave(() -> bed.acceptReservation
                .accept(new AcceptReservationRequest(requested.id().value(), KEMAS_HOME_PROVIDER_ID)));

        assertThatThrownBy(() -> decline(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.publisher.published()).hasSize(1).first().isInstanceOf(ReservationReservedIE.class);
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("When another save moved a schedule on meanwhile, the decline reloads and is saved on the next try; after three conflicts it fails and nothing changes.")
    void declineIsRetriedAfterAConflictAndGivesUpAfterThree() {
        bed.publisher.clear();
        bed.time.set(monday(21, 0));
        bed.reservations.conflictOnNextSaves(3);

        assertThatThrownBy(() -> decline(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(ScheduleChangedException.class);
        assertStillRequested();

        bed.reservations.conflictOnNextSaves(2);
        decline(KEMAS_HOME_PROVIDER_ID);

        assertThat(bed.reload(requested).status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(bed.publisher.single(ReservationDeclinedIE.class).declinedAt()).isEqualTo(monday(21, 0));
    }
}
