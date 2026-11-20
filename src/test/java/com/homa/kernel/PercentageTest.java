package com.homa.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PercentageTest {

    @Test
    void PAY_3_platform_fee_of_15_5_percent_on_200_is_31() {
        assertThat(Percentage.of("15.5").of(Money.myr(200))).isEqualTo(Money.myr(31));
    }

    @Test
    void NRS_2_twelve_percent_of_200_is_24_exactly() {
        assertThat(Percentage.of(12).of(Money.myr(200)).amount()).isEqualByComparingTo("24");
    }

    @Test
    void a_percentage_of_an_amount_is_exact() {
        assertThat(Percentage.of(3).of(Money.myr("10.01")).amount()).isEqualByComparingTo("0.3003");
    }

    @Test
    void OFR_4_complement_of_a_10_percent_discount_is_90_percent() {
        assertThat(Percentage.of(10).complement()).isEqualTo(Percentage.of(90));
        assertThat(Percentage.ZERO.complement()).isEqualTo(Percentage.HUNDRED_PERCENT);
        assertThat(Percentage.HUNDRED_PERCENT.complement()).isEqualTo(Percentage.ZERO);
    }

    @Test
    void as_fraction_moves_the_point_two_places() {
        assertThat(Percentage.of("15.5").asFraction()).isEqualByComparingTo("0.155");
        assertThat(Percentage.of(100).asFraction()).isEqualByComparingTo("1");
    }

    @Test
    void bounds_are_0_and_100_inclusive() {
        assertThat(Percentage.of(0).value()).isEqualByComparingTo("0");
        assertThat(Percentage.of(100).value()).isEqualByComparingTo("100");
        assertThatThrownBy(() -> Percentage.of("-0.01")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-0.01");
        assertThatThrownBy(() -> Percentage.of("100.01")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Percentage.of((BigDecimal) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void zero_is_reported() {
        assertThat(Percentage.ZERO.isZero()).isTrue();
        assertThat(Percentage.of("0.01").isZero()).isFalse();
    }

    @Test
    void equal_values_are_equal_whatever_their_scale() {
        assertThat(Percentage.of("10")).isEqualTo(Percentage.of("10.0"));
        assertThat(Percentage.of("10")).hasSameHashCodeAs(Percentage.of("10.00"));
        assertThat(Percentage.of("10").hashCode()).isNotEqualTo(Percentage.of("11").hashCode());
        assertThat(Percentage.of(BigDecimal.TEN)).isEqualTo(Percentage.of(10));
        assertThat(Percentage.of("10")).isNotEqualTo(Percentage.of("10.5"));
        assertThat(Percentage.of("10")).isNotEqualTo("10%");
    }

    @Test
    void percentages_compare_by_value() {
        assertThat(Percentage.of(10).compareTo(Percentage.of(30))).isNegative();
        assertThat(Percentage.of(30).compareTo(Percentage.of(10))).isPositive();
        assertThat(Percentage.of(30).compareTo(Percentage.of("30.0"))).isZero();
    }

    @Test
    void prints_without_trailing_zeros() {
        assertThat(Percentage.of("15.50")).hasToString("15.5%");
        assertThat(Percentage.of("100")).hasToString("100%");
    }
}
