package com.homa.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency SGD = Currency.getInstance("SGD");

    @Test
    void PAY_8_arithmetic_stays_exact_until_a_line_is_rounded() {
        Money refund = Money.myr("22.50").times(new BigDecimal("1.75"));

        assertThat(refund.amount()).isEqualByComparingTo("39.375");
        assertThat(refund.roundedToSen()).isEqualTo(Money.myr("39.38"));
        assertThat(refund.roundedToSen().amount().scale()).isEqualTo(2);
    }

    @Test
    void PAY_8_rounds_half_up_to_the_sen() {
        assertThat(Money.myr("10.005").roundedToSen().amount()).isEqualByComparingTo("10.01");
        assertThat(Money.myr("10.004").roundedToSen().amount()).isEqualByComparingTo("10.00");
        assertThat(Money.myr("-10.005").roundedToSen().amount()).isEqualByComparingTo("-10.01");
    }

    @Test
    void PAY_8_refund_is_paid_minus_the_rounded_fee_so_lines_add_up() {
        Money paid = Money.myr(200);
        Money fee = Money.myr("39.375").roundedToSen();

        assertThat(paid.minus(fee)).isEqualTo(Money.myr("160.62"));
        assertThat(paid.minus(fee).plus(fee)).isEqualTo(paid);
    }

    @Test
    void hours_fee_of_the_standard_reservation_is_180() {
        Money hoursFee = Money.myr(25).times(4).times(2).times(new BigDecimal("0.9"));

        assertThat(hoursFee).isEqualTo(Money.myr(180));
        assertThat(hoursFee.plus(Money.myr(20))).isEqualTo(Money.myr(200));
    }

    @Test
    void equal_amounts_are_equal_whatever_their_scale() {
        assertThat(Money.myr("45")).isEqualTo(Money.myr("45.00"));
        assertThat(Money.myr("45")).hasSameHashCodeAs(Money.myr("45.00"));
        assertThat(Money.myr("45").hashCode()).isNotEqualTo(Money.myr("46").hashCode());
        assertThat(Money.myr("45")).isNotEqualTo(Money.myr("45.01"));
        assertThat(Money.myr("45")).isNotEqualTo(Money.of(new BigDecimal("45"), SGD));
        assertThat(Money.myr("45")).isNotEqualTo("MYR 45");
    }

    @Test
    void factories_give_the_same_value() {
        assertThat(Money.myr(45)).isEqualTo(Money.myr("45"));
        assertThat(Money.of(new BigDecimal("45"), Money.MYR)).isEqualTo(Money.myr(45));
        assertThat(Money.zero(Money.MYR)).isEqualTo(Money.myr(0));
        assertThat(Money.myr(45).currency()).isEqualTo(Money.MYR);
    }

    @Test
    void signs_are_reported() {
        assertThat(Money.myr(0).isZero()).isTrue();
        assertThat(Money.myr(1).isZero()).isFalse();
        assertThat(Money.myr(-1).isNegative()).isTrue();
        assertThat(Money.myr(0).isNegative()).isFalse();
        assertThat(Money.myr(1).isPositive()).isTrue();
        assertThat(Money.myr(0).isPositive()).isFalse();
    }

    @Test
    void amounts_compare_by_value() {
        Money small = Money.myr(45);
        Money large = Money.myr(200);

        assertThat(large.isGreaterThan(small)).isTrue();
        assertThat(small.isGreaterThan(large)).isFalse();
        assertThat(small.isGreaterThan(Money.myr("45.00"))).isFalse();
        assertThat(small.isLessThan(large)).isTrue();
        assertThat(large.isLessThan(small)).isFalse();
        assertThat(small.isLessThan(Money.myr("45.00"))).isFalse();
        assertThat(small.compareTo(large)).isNegative();
    }

    @Test
    void CAN_2_a_fee_is_capped_with_min() {
        assertThat(Money.myr(250).min(Money.myr(200))).isEqualTo(Money.myr(200));
        assertThat(Money.myr(45).min(Money.myr(200))).isEqualTo(Money.myr(45));
        assertThat(Money.myr(45).max(Money.myr(200))).isEqualTo(Money.myr(200));
        assertThat(Money.myr(250).max(Money.myr(200))).isEqualTo(Money.myr(250));
    }

    @Test
    void min_and_max_of_equal_amounts_keep_the_receiver() {
        Money a = Money.myr("45");
        Money b = Money.myr("45.00");

        assertThat(a.min(b)).isSameAs(a);
        assertThat(a.max(b)).isSameAs(a);
    }

    @Test
    void currencies_cannot_be_mixed() {
        Money sgd = Money.of(BigDecimal.ONE, SGD);

        assertThatThrownBy(() -> Money.myr(1).plus(sgd)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.myr(1).minus(sgd)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.myr(1).compareTo(sgd)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MYR").hasMessageContaining("SGD");
    }

    @Test
    void nulls_are_refused() {
        assertThatThrownBy(() -> Money.of(null, Money.MYR)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.myr(1).plus(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.myr(1).times((BigDecimal) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void prints_currency_and_plain_amount() {
        assertThat(Money.myr("39.38")).hasToString("MYR 39.38");
        assertThat(Money.myr("1E+2").toString()).isEqualTo("MYR 100");
    }
}
