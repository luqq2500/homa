package com.homa.reservation.domain;

import static com.homa.reservation.domain.ReservationFixtures.ACCEPTED_AT;
import static com.homa.reservation.domain.ReservationFixtures.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.AcceptanceDeadlinePassedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReservationAcceptanceTest {

    private final Marketplace market = Marketplace.withKemasHome();

    @Test
    @Tag("ACC-2")
    @DisplayName("Aisyah pays on Monday 20:00. Kemas Home accepts on Monday 22:00, and the reservation becomes Reserved.")
    void requestBecomesReservedWhenTheProviderAcceptsBeforeTheDeadline() {
        Reservation reservation = market.requestStandard();

        reservation.accept(ACCEPTED_AT);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.reservedAt()).contains(ACCEPTED_AT);
    }

    @Test
    @Tag("ACC-2")
    @DisplayName("Kemas Home can still accept at Tuesday 19:59, one minute before the deadline, but not at 20:00.")
    void providerCanAcceptUntilTheMinuteBeforeTheDeadline() {
        Reservation inTime = market.requestStandard();
        inTime.accept(tuesday(19, 59));
        assertThat(inTime.status()).isEqualTo(ReservationStatus.RESERVED);

        Reservation tooLate = Marketplace.withKemasHome().requestStandard();
        assertThatThrownBy(() -> tooLate.accept(tuesday(20, 0))).isInstanceOf(AcceptanceDeadlinePassedException.class);
        assertThat(tooLate.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(tooLate.reservedAt()).isEmpty();
    }

    @Test
    @Tag("ACC-2")
    @DisplayName("Only a Requested reservation can be accepted.")
    void reservedReservationCannotBeAcceptedAgain() {
        Reservation reservation = market.reserveStandard();

        assertThatThrownBy(() -> reservation.accept(tuesday(9, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThat(reservation.reservedAt()).contains(ACCEPTED_AT);
    }
}
