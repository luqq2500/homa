package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.ACCEPTED_AT;
import static com.homa.reservation.domain.ReservationFixtures.meiBooking;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.standardBooking;
import static com.homa.reservation.domain.ReservationFixtures.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.AcceptanceDeadlineNotReachedException;
import com.homa.reservation.domain.exception.AcceptanceDeadlinePassedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReservationDeclineAndExpiryTest {

    private final Marketplace market = Marketplace.withKemasHome();

    @Test
    @Tag("REQ-3")
    @DisplayName("Kemas Home declines because two helpers are on leave. Aisyah gets RM200 back.")
    void providerDeclinesARequestAndTheTimeOpensAgain() {
        Reservation reservation = market.requestStandard();

        reservation.decline(monday(21, 0));
        market.providerSchedule().release(reservation);
        market.scheduleOf(AISYAH_CUSTOMER_ID).release(reservation);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(reservation.declinedAt()).contains(monday(21, 0));
        assertThat(reservation.withdrawnAt()).isEmpty();
        assertThat(reservation.expiredAt()).isEmpty();
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(market.providerSchedule().blocks()).isEmpty();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).blocks()).isEmpty();
        assertThat(market.book(meiBooking(), monday(21, 1), monday(21, 2)).status())
                .isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("Only a Requested reservation can be declined: not one that is already Reserved, Withdrawn, Declined or Expired.")
    void onlyARequestedReservationCanBeDeclined() {
        Reservation reserved = market.reserveStandard();
        Reservation withdrawn = Marketplace.withKemasHome().requestStandard();
        withdrawn.withdraw(monday(21, 0));
        Reservation declined = Marketplace.withKemasHome().requestStandard();
        declined.decline(monday(21, 0));
        Reservation expired = Marketplace.withKemasHome().requestStandard();
        expired.expire(tuesday(20, 0));

        for (Reservation reservation : new Reservation[] {reserved, withdrawn, declined, expired}) {
            ReservationStatus before = reservation.status();
            assertThatThrownBy(() -> reservation.decline(tuesday(21, 0)))
                    .isInstanceOf(ReservationStatusConflictException.class);
            assertThat(reservation.status()).isEqualTo(before);
            assertThat(reservation.declinedAt()).isEqualTo(before == ReservationStatus.DECLINED
                    ? java.util.Optional.of(monday(21, 0)) : java.util.Optional.empty());
        }
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Had Kemas Home's decline been recorded first, Aisyah's withdrawal and its own acceptance would be refused, and the reservation stays Declined.")
    void declineRecordedFirstWinsOverALaterAcceptanceOrWithdrawal() {
        Reservation reservation = market.requestStandard();
        reservation.decline(monday(21, 2));

        assertThatThrownBy(() -> reservation.accept(ACCEPTED_AT)).isInstanceOf(ReservationStatusConflictException.class);
        assertThatThrownBy(() -> reservation.withdraw(monday(21, 3)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(reservation.reservedAt()).isEmpty();
        assertThat(reservation.withdrawnAt()).isEmpty();
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("Kemas Home doesn't respond by Tuesday 20:00. Aisyah gets RM200 back.")
    void requestExpiresWhenTheProviderMissesTheAcceptanceDeadline() {
        Reservation reservation = market.requestStandard();

        reservation.expire(tuesday(20, 0));
        market.providerSchedule().release(reservation);
        market.scheduleOf(AISYAH_CUSTOMER_ID).release(reservation);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(reservation.expiredAt()).contains(tuesday(20, 0));
        assertThat(reservation.declinedAt()).isEmpty();
        assertThat(reservation.withdrawnAt()).isEmpty();
        assertThat(reservation.reservedAt()).isEmpty();
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(market.providerSchedule().blocks()).isEmpty();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).blocks()).isEmpty();
        assertThat(market.book(meiBooking(), tuesday(20, 1), tuesday(20, 2)).status())
                .isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("A minute before Tuesday 20:00 the request is still open: it can be accepted and can't yet expire. From 20:00 it can expire and can't be accepted; a minute later too.")
    void requestExpiresExactlyWhenItCanNoLongerBeAccepted() {
        Reservation openRequest = market.requestStandard();

        assertThatThrownBy(() -> openRequest.expire(tuesday(19, 59)))
                .isInstanceOf(AcceptanceDeadlineNotReachedException.class);
        assertThat(openRequest.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(openRequest.expiredAt()).isEmpty();
        openRequest.accept(tuesday(19, 59));
        assertThat(openRequest.status()).isEqualTo(ReservationStatus.RESERVED);

        Reservation atDeadline = Marketplace.withKemasHome().requestStandard();
        assertThatThrownBy(() -> atDeadline.accept(tuesday(20, 0)))
                .isInstanceOf(AcceptanceDeadlinePassedException.class);
        atDeadline.expire(tuesday(20, 0));
        assertThat(atDeadline.status()).isEqualTo(ReservationStatus.EXPIRED);

        Reservation aMinuteLate = Marketplace.withKemasHome().requestStandard();
        aMinuteLate.expire(tuesday(20, 1));
        assertThat(aMinuteLate.expiredAt()).contains(tuesday(20, 1));
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("A Saturday 7:00 request for 10:00 has until 8:45, and then it expires.")
    void aLateRequestExpiresAnHourBeforeTheCheckInPeriod() {
        Reservation reservation = market.book(standardBooking(), saturday(6, 58), saturday(7, 0));

        assertThatThrownBy(() -> reservation.expire(saturday(8, 44)))
                .isInstanceOf(AcceptanceDeadlineNotReachedException.class);
        reservation.expire(saturday(8, 45));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(Duration.between(reservation.expiredAt().orElseThrow(), saturday(9, 45)))
                .isEqualTo(Duration.ofHours(1));
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("Only a Requested reservation can expire: not one that is Reserved, Withdrawn, Declined or already Expired.")
    void onlyARequestedReservationCanExpire() {
        Reservation reserved = market.reserveStandard();
        Reservation withdrawn = Marketplace.withKemasHome().requestStandard();
        withdrawn.withdraw(monday(21, 0));
        Reservation declined = Marketplace.withKemasHome().requestStandard();
        declined.decline(monday(21, 0));
        Reservation expired = Marketplace.withKemasHome().requestStandard();
        expired.expire(tuesday(20, 0));

        for (Reservation reservation : new Reservation[] {reserved, withdrawn, declined, expired}) {
            ReservationStatus before = reservation.status();
            assertThatThrownBy(() -> reservation.expire(tuesday(21, 0)))
                    .isInstanceOf(ReservationStatusConflictException.class);
            assertThat(reservation.status()).isEqualTo(before);
        }
        assertThat(expired.expiredAt()).contains(tuesday(20, 0));
        assertThat(reserved.expiredAt()).isEmpty();
    }
}
