package com.homa.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReservationIdentityTest {

    @Test
    @Tag("RES-7")
    @DisplayName("Every reservation and every checkout gets an identity of its own.")
    void reservationsAndCheckoutsGetDistinctIdentities() {
        assertThat(ReservationId.generate().value()).isNotBlank();
        assertThat(ReservationId.generate()).isNotEqualTo(ReservationId.generate());
        assertThat(CheckoutId.generate().value()).isNotBlank();
        assertThat(CheckoutId.generate()).isNotEqualTo(CheckoutId.generate());
    }

    @Test
    @Tag("RES-9")
    @DisplayName("Only Requested and Reserved reservations hold the calendar; Withdrawn, Declined, Expired, Cancelled and Completed don't.")
    void onlyOpenStatusesHoldTheCalendar() {
        assertThat(ReservationStatus.REQUESTED.holdsCalendar()).isTrue();
        assertThat(ReservationStatus.RESERVED.holdsCalendar()).isTrue();
        assertThat(ReservationStatus.COMPLETED.holdsCalendar()).isFalse();
        assertThat(ReservationStatus.WITHDRAWN.holdsCalendar()).isFalse();
        assertThat(ReservationStatus.DECLINED.holdsCalendar()).isFalse();
        assertThat(ReservationStatus.EXPIRED.holdsCalendar()).isFalse();
        assertThat(ReservationStatus.CANCELLED.holdsCalendar()).isFalse();
    }
}
