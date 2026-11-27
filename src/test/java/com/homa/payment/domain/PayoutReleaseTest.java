package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.payment.domain.PaymentFixtures.MARKED_COMPLETED_AT;
import static com.homa.payment.domain.PaymentFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.domain.PaymentFixtures.jobPayoutOf;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesCancellationFeePayout;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesJobPayout;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesPayoutAtReserved;
import static com.homa.payment.domain.PaymentFixtures.payoutAtReservedOf;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PayoutReleaseTest {

    /** A made-up later change: from Tuesday 1 December 2026 the platform fee is 18%. */
    private static final Instant FEE_RAISED_FROM = at(2026, 12, 1, 0, 0);
    private static final PaymentValues FEE_RAISED_ON_TUESDAY = new PaymentValues(List.of(
            PaymentValues.current(),
            new PaymentValues.Version(FEE_RAISED_FROM, Percentage.of(18), Percentage.of(20))));

    @Test
    @Tag("PAY-3")
    @DisplayName("A RM200 payout pays Kemas Home RM169.")
    void platformFeeIsTakenFromAJobPayout() {
        PayoutRelease release = kemasHomesJobPayout().release(PROTECTION_WINDOW_END);

        assertThat(release.kind()).isEqualTo(PayoutKind.JOB);
        assertThat(release.gross()).isEqualTo(Money.myr(200));
        assertThat(release.platformFee()).isEqualTo(Money.myr(31));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr(169));
    }

    @Test
    @Tag("PAY-3")
    @Tag("PAY-6")
    @DisplayName("A RM45 cancellation fee pays it RM38.02 after a RM6.98 platform fee.")
    void platformFeeIsTakenFromAFeePayoutToo() {
        PayoutRelease release = kemasHomesCancellationFeePayout().release(PROTECTION_WINDOW_END);

        assertThat(release.kind()).isEqualTo(PayoutKind.FEE);
        assertThat(release.gross()).isEqualTo(Money.myr(45));
        assertThat(release.platformFee()).isEqualTo(Money.myr("6.98"));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr("38.02"));
    }

    @Test
    @Tag("PAY-8")
    @Tag("RPT-9")
    @DisplayName("Nurul leaves at 12:05 and the reservation total becomes RM160.62 before the platform fee: "
            + "15.5% is RM24.8961, rounded once to RM24.90, so Kemas Home receives RM135.72.")
    void platformFeeIsRoundedOnceToTheSenAndTheLinesAddUp() {
        Payout payout = kemasHomesPayoutAtReserved();
        payout.reduceByRefund(Money.myr("39.38"));
        payout.markCompleted(MARKED_COMPLETED_AT);

        PayoutRelease release = payout.release(PROTECTION_WINDOW_END);

        assertThat(release.gross()).isEqualTo(Money.myr("160.62"));
        assertThat(release.platformFee()).isEqualTo(Money.myr("24.90"));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr("135.72"));
        assertThat(release.afterPlatformFee().plus(release.platformFee())).isEqualTo(release.gross());
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A RM100.10 payout gives a RM15.5155 fee, rounded half up once to RM15.52.")
    void platformFeeRoundsHalfUpToTheSen() {
        PayoutRelease release = jobPayoutOf(Money.myr("100.10")).release(PROTECTION_WINDOW_END);

        assertThat(release.platformFee()).isEqualTo(Money.myr("15.52"));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr("84.58"));
    }

    @Test
    @Tag("RES-8")
    @Tag("PAY-3")
    @DisplayName("Homa raises the platform fee from Tuesday. Aisyah's reservation, Requested on Monday 20:00, keeps "
            + "15.5%: Kemas Home still receives RM169.")
    void laterPlatformFeeDoesNotChangeAReservationRequestedEarlier() {
        PayoutRelease release = kemasHomesJobPayout().release(PROTECTION_WINDOW_END, FEE_RAISED_ON_TUESDAY);

        assertThat(release.platformFee()).isEqualTo(Money.myr(31));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr(169));
    }

    @Test
    @Tag("RES-8")
    @Tag("PAY-3")
    @DisplayName("A reservation Requested on Tuesday, after Homa raised the platform fee, pays the new rate: "
            + "18% of RM200 is RM36.")
    void reservationRequestedAfterAChangePaysTheNewPlatformFee() {
        Payout payout = payoutAtReservedOf(TOTAL_RESERVATION_FEE, FEE_RAISED_FROM);
        payout.markCompleted(MARKED_COMPLETED_AT);

        PayoutRelease release = payout.release(PROTECTION_WINDOW_END, FEE_RAISED_ON_TUESDAY);

        assertThat(release.platformFee()).isEqualTo(Money.myr(36));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr(164));
        assertThat(REQUESTED_AT).isBefore(FEE_RAISED_FROM);
    }
}
