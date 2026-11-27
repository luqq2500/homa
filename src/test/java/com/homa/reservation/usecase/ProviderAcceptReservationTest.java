package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.ACCEPTED_AT;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.AcceptanceDeadlinePassedException;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.AcceptReservationResponse;
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
}
