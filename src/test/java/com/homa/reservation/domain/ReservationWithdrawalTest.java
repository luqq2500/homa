package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.fixtures.HomaFixtures.STANDARD_HELPERS;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.reservation.domain.ReservationFixtures.ACCEPTED_AT;
import static com.homa.reservation.domain.ReservationFixtures.aisyahBooking;
import static com.homa.reservation.domain.ReservationFixtures.meiBooking;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.terms.AcceptanceDeadline;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReservationWithdrawalTest {

    private final Marketplace market = Marketplace.withKemasHome();

    @Test
    @Tag("REQ-2")
    @DisplayName("Aisyah withdraws on Monday 21:00 before Kemas Home responds. She gets the full RM200 back, and Saturday 10:00–14:00 opens for other customers.")
    void customerWithdrawsARequestForFreeAndTheTimeOpensAgain() {
        Reservation reservation = market.requestStandard();

        reservation.withdraw(monday(21, 0));
        market.providerSchedule().release(reservation);
        market.scheduleOf(AISYAH_CUSTOMER_ID).release(reservation);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.WITHDRAWN);
        assertThat(reservation.withdrawnAt()).contains(monday(21, 0));
        assertThat(reservation.quote().total()).isEqualTo(TOTAL_RESERVATION_FEE);
        assertThat(market.providerSchedule().blocks()).isEmpty();
        assertThat(market.scheduleOf(AISYAH_CUSTOMER_ID).blocks()).isEmpty();
        assertThat(market.book(meiBooking(), monday(21, 1), monday(21, 2)).status())
                .isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    @Tag("REQ-1")
    @DisplayName("On Monday 21:00 Aisyah wants 3 hours instead of 4. She withdraws her request, gets RM200 back, and requests 3 hours for RM155. It's a new request with a new acceptance deadline, Tuesday 21:00.")
    void changeIsAWithdrawalAndANewRequest() {
        Reservation fourHours = market.requestStandard();
        fourHours.withdraw(monday(21, 0));
        market.providerSchedule().release(fourHours);
        market.scheduleOf(AISYAH_CUSTOMER_ID).release(fourHours);

        Reservation threeHours = market.book(aisyahBooking(JOB_START, Duration.ofHours(3), STANDARD_HELPERS),
                monday(20, 59), monday(21, 0));

        assertThat(threeHours.id()).isNotEqualTo(fourHours.id());
        assertThat(threeHours.quote().total()).isEqualTo(Money.myr(155));
        assertThat(threeHours.acceptanceDeadline()).contains(new AcceptanceDeadline(tuesday(21, 0)));
        assertThat(Arrays.stream(Reservation.class.getMethods()).map(java.lang.reflect.Method::getName))
                .doesNotContain("edit");
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Kemas Home's acceptance is recorded at 21:02:10 and Aisyah's withdrawal at 21:02:12. The reservation is Reserved, so she can't withdraw it and is told she can cancel instead.")
    void acceptanceRecordedFirstWinsOverALaterWithdrawal() {
        Reservation reservation = market.requestStandard();
        reservation.accept(monday(21, 2).plusSeconds(10));

        assertThatThrownBy(() -> reservation.withdraw(monday(21, 2).plusSeconds(12)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.withdrawnAt()).isEmpty();
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Had Aisyah's withdrawal been recorded first, Kemas Home's acceptance would be refused and the reservation stays Withdrawn.")
    void withdrawalRecordedFirstWinsOverALaterAcceptance() {
        Reservation reservation = market.requestStandard();
        reservation.withdraw(monday(21, 2));

        assertThatThrownBy(() -> reservation.accept(ACCEPTED_AT)).isInstanceOf(ReservationStatusConflictException.class);
        assertThatThrownBy(() -> reservation.withdraw(monday(21, 3)))
                .isInstanceOf(ReservationStatusConflictException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.WITHDRAWN);
        assertThat(reservation.withdrawnAt()).contains(monday(21, 2));
        assertThat(reservation.reservedAt()).isEmpty();
    }
}
