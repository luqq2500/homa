package com.homa.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PlatformFeeRateTest {

    @Test
    @Tag("PAY-3")
    @DisplayName("The platform fee is 15.5% of the provider's released payout for a reservation.")
    void platformFeeRateIsFifteenAndAHalfPercent() {
        assertThat(PlatformFeeRate.HOMA.percentage()).isEqualTo(HomaFixtures.PLATFORM_FEE_RATE);
    }

    @Test
    @Tag("PAY-3")
    @Tag("PAY-8")
    @DisplayName("Payout RM200: platform fee RM31. Payout RM160.62: platform fee RM24.90, rounded once to the sen.")
    void platformFeeOnAPayoutIsRoundedOnceToTheSen() {
        assertThat(PlatformFeeRate.HOMA.feeOn(Money.myr(200))).isEqualTo(Money.myr(31));
        Money fee = PlatformFeeRate.HOMA.feeOn(Money.myr("160.62"));

        assertThat(fee.amount()).isEqualByComparingTo("24.90");
        assertThat(fee.amount().scale()).isEqualTo(2);
    }
}
