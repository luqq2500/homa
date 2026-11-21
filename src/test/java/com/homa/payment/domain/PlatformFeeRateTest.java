package com.homa.payment.domain;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.fixtures.HomaFixtures;
import com.homa.kernel.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PlatformFeeRateTest {

    @Test
    @Tag("PAY-3")
    @DisplayName("The platform fee is 15.5% of every payout to a provider.")
    void platformFeeRateIsReadFromPaymentValues() {
        assertThat(PlatformFeeRate.asOf(REQUESTED_AT).percentage()).isEqualTo(HomaFixtures.PLATFORM_FEE_RATE);
        assertThat(PlatformFeeRate.asOf(REQUESTED_AT).percentage())
                .isEqualTo(PaymentValues.asOf(REQUESTED_AT).platformFeeRate());
    }

    @Test
    @Tag("PAY-3")
    @Tag("PAY-8")
    @DisplayName("Payout RM200: platform fee RM31. A RM45 fee: RM6.975, rounded once to RM6.98.")
    void platformFeeOnAPayoutIsRoundedOnceToTheSen() {
        PlatformFeeRate rate = PlatformFeeRate.asOf(REQUESTED_AT);

        assertThat(rate.feeOn(Money.myr(200))).isEqualTo(Money.myr(31));
        Money fee = rate.feeOn(Money.myr(45));

        assertThat(fee.amount()).isEqualByComparingTo("6.98");
        assertThat(fee.amount().scale()).isEqualTo(2);
    }
}
