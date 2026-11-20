package com.homa.payment.domain;

import static com.homa.payment.domain.PaymentFixtures.PROTECTION_WINDOW_END;
import static com.homa.payment.domain.PaymentFixtures.jobPayoutOf;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesCancellationFeePayout;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesJobPayout;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PayoutReleaseTest {

    @Test
    @Tag("PAY-3")
    @DisplayName("Payout RM200: platform fee RM31, Kemas Home receives RM169.")
    void platformFeeIsFifteenAndAHalfPercentOfAJobPayout() {
        PayoutRelease release = kemasHomesJobPayout().release(PROTECTION_WINDOW_END);

        assertThat(release.kind()).isEqualTo(PayoutKind.JOB);
        assertThat(release.gross()).isEqualTo(Money.myr(200));
        assertThat(release.platformFee()).isEqualTo(Money.myr(31));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr(169));
    }

    @Test
    @Tag("PAY-3")
    @DisplayName("If Aisyah had paid a RM45 cancellation fee instead, Kemas Home would receive the full RM45.")
    void noPlatformFeeIsTakenFromAFeePayout() {
        PayoutRelease release = kemasHomesCancellationFeePayout().release(PROTECTION_WINDOW_END);

        assertThat(release.kind()).isEqualTo(PayoutKind.FEE);
        assertThat(release.platformFee()).isEqualTo(Money.myr(0));
        assertThat(release.afterPlatformFee()).isEqualTo(Money.myr(45));
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("Nurul leaves at 12:05 and the reservation total becomes RM160.62 before the platform fee: "
            + "15.5% is RM24.8961, rounded once to RM24.90, so Kemas Home receives RM135.72.")
    void platformFeeIsRoundedOnceToTheSenAndTheLinesAddUp() {
        PayoutRelease release = jobPayoutOf(Money.myr("160.62")).release(PROTECTION_WINDOW_END);

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
}
