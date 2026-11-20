package com.homa.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PercentageTest {

    @Test
    @Tag("PAY-3")
    @DisplayName("Payout RM200: platform fee RM31, Kemas Home receives RM169.")
    void platformFeeOnA200PayoutIs31() {
        assertThat(Percentage.of("15.5").of(Money.myr(200))).isEqualTo(Money.myr(31));
    }

    @Test
    @Tag("NRS-2")
    @DisplayName("No roster: the fine adds 12% of RM200.")
    void twelvePercentOfTheTotalIs24() {
        assertThat(Percentage.of(12).of(Money.myr(200)).amount()).isEqualByComparingTo("24");
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("A percentage of an amount is not rounded until a line is produced.")
    void percentageOfAnAmountIsExact() {
        assertThat(Percentage.of(3).of(Money.myr("10.01")).amount()).isEqualByComparingTo("0.3003");
    }

    @Test
    @Tag("OFR-4")
    @DisplayName("A 10% discount leaves 90% of the price.")
    void complementOfATenPercentDiscountIsNinetyPercent() {
        assertThat(Percentage.of(10).complement()).isEqualTo(Percentage.of(90));
        assertThat(Percentage.ZERO.complement()).isEqualTo(Percentage.HUNDRED_PERCENT);
        assertThat(Percentage.HUNDRED_PERCENT.complement()).isEqualTo(Percentage.ZERO);
    }

    @Test
    @DisplayName("15.5% is 0.155.")
    void percentageAsFractionMovesThePointTwoPlaces() {
        assertThat(Percentage.of("15.5").asFraction()).isEqualByComparingTo("0.155");
        assertThat(Percentage.of(100).asFraction()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("A percentage is from 0 to 100.")
    void percentageIsFromZeroToOneHundred() {
        assertThat(Percentage.of(0).value()).isEqualByComparingTo("0");
        assertThat(Percentage.of(100).value()).isEqualByComparingTo("100");
        assertThatThrownBy(() -> Percentage.of("-0.01")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-0.01");
        assertThatThrownBy(() -> Percentage.of("100.01")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Percentage.of((BigDecimal) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("0% is zero.")
    void zeroPercentIsReported() {
        assertThat(Percentage.ZERO.isZero()).isTrue();
        assertThat(Percentage.of("0.01").isZero()).isFalse();
    }

    @Test
    @DisplayName("10% equals 10.0%.")
    void equalPercentagesAreEqualWhateverTheirScale() {
        assertThat(Percentage.of("10")).isEqualTo(Percentage.of("10.0"));
        assertThat(Percentage.of("10")).hasSameHashCodeAs(Percentage.of("10.00"));
        assertThat(Percentage.of("10").hashCode()).isNotEqualTo(Percentage.of("11").hashCode());
        assertThat(Percentage.of(BigDecimal.TEN)).isEqualTo(Percentage.of(10));
        assertThat(Percentage.of("10")).isNotEqualTo(Percentage.of("10.5"));
        assertThat(Percentage.of("10")).isNotEqualTo("10%");
    }

    @Test
    @DisplayName("10% is less than 30%.")
    void percentagesCompareByValue() {
        assertThat(Percentage.of(10).compareTo(Percentage.of(30))).isNegative();
        assertThat(Percentage.of(30).compareTo(Percentage.of(10))).isPositive();
        assertThat(Percentage.of(30).compareTo(Percentage.of("30.0"))).isZero();
    }

    @Test
    @DisplayName("15.50% prints as 15.5%.")
    void percentagePrintsWithoutTrailingZeros() {
        assertThat(Percentage.of("15.50")).hasToString("15.5%");
        assertThat(Percentage.of("100")).hasToString("100%");
    }
}
