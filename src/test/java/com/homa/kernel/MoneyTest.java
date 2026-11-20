package com.homa.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency SGD = Currency.getInstance("SGD");

    @Test
    @Tag("PAY-8")
    @DisplayName("Nurul leaves at 12:05: the refund is 1.75 × 22.50 = RM39.375, rounded once to RM39.38.")
    void arithmeticStaysExactUntilALineIsRounded() {
        Money refund = Money.myr("22.50").times(new BigDecimal("1.75"));

        assertThat(refund.amount()).isEqualByComparingTo("39.375");
        assertThat(refund.roundedToSen()).isEqualTo(Money.myr("39.38"));
        assertThat(refund.roundedToSen().amount().scale()).isEqualTo(2);
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("Each money line is rounded once to the nearest sen, half up.")
    void lineRoundsHalfUpToTheSen() {
        assertThat(Money.myr("10.005").roundedToSen().amount()).isEqualByComparingTo("10.01");
        assertThat(Money.myr("10.004").roundedToSen().amount()).isEqualByComparingTo("10.00");
        assertThat(Money.myr("-10.005").roundedToSen().amount()).isEqualByComparingTo("-10.01");
    }

    @Test
    @Tag("PAY-8")
    @DisplayName("The reservation total becomes RM200 − RM39.38 = RM160.62, so the lines always add up.")
    void refundIsAmountPaidMinusTheRoundedFee() {
        Money paid = Money.myr(200);
        Money fee = Money.myr("39.375").roundedToSen();

        assertThat(paid.minus(fee)).isEqualTo(Money.myr("160.62"));
        assertThat(paid.minus(fee).plus(fee)).isEqualTo(paid);
    }

    @Test
    @Tag("RES-6")
    @DisplayName("25 × 4 hours × 2 helpers × (1 − 10%) = RM180, plus RM20 travel = RM200.")
    void standardReservationHoursFeeIs180AndTotalIs200() {
        Money hoursFee = Money.myr(25).times(4).times(2).times(new BigDecimal("0.9"));

        assertThat(hoursFee).isEqualTo(Money.myr(180));
        assertThat(hoursFee.plus(Money.myr(20))).isEqualTo(Money.myr(200));
    }

    @Test
    @DisplayName("RM45 equals RM45.00.")
    void equalAmountsAreEqualWhateverTheirScale() {
        assertThat(Money.myr("45")).isEqualTo(Money.myr("45.00"));
        assertThat(Money.myr("45")).hasSameHashCodeAs(Money.myr("45.00"));
        assertThat(Money.myr("45").hashCode()).isNotEqualTo(Money.myr("46").hashCode());
        assertThat(Money.myr("45")).isNotEqualTo(Money.myr("45.01"));
        assertThat(Money.myr("45")).isNotEqualTo(Money.of(new BigDecimal("45"), SGD));
        assertThat(Money.myr("45")).isNotEqualTo("MYR 45");
    }

    @Test
    @DisplayName("Every way of writing RM45 gives the same amount.")
    void factoriesGiveTheSameValue() {
        assertThat(Money.myr(45)).isEqualTo(Money.myr("45"));
        assertThat(Money.of(new BigDecimal("45"), Money.MYR)).isEqualTo(Money.myr(45));
        assertThat(Money.zero(Money.MYR)).isEqualTo(Money.myr(0));
        assertThat(Money.myr(45).currency()).isEqualTo(Money.MYR);
    }

    @Test
    @DisplayName("An amount knows whether it is zero, negative or positive.")
    void amountReportsItsSign() {
        assertThat(Money.myr(0).isZero()).isTrue();
        assertThat(Money.myr(1).isZero()).isFalse();
        assertThat(Money.myr(-1).isNegative()).isTrue();
        assertThat(Money.myr(0).isNegative()).isFalse();
        assertThat(Money.myr(1).isPositive()).isTrue();
        assertThat(Money.myr(0).isPositive()).isFalse();
    }

    @Test
    @DisplayName("RM200 is more than RM45.")
    void amountsCompareByValue() {
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
    @Tag("CAN-2")
    @DisplayName("All fees and fines are capped at 100% of the Total Reservation Fee.")
    void feeIsCappedAtTheTotalWithMin() {
        assertThat(Money.myr(250).min(Money.myr(200))).isEqualTo(Money.myr(200));
        assertThat(Money.myr(45).min(Money.myr(200))).isEqualTo(Money.myr(45));
        assertThat(Money.myr(45).max(Money.myr(200))).isEqualTo(Money.myr(200));
        assertThat(Money.myr(250).max(Money.myr(200))).isEqualTo(Money.myr(250));
    }

    @Test
    @DisplayName("Capping an amount at an equal amount leaves it unchanged.")
    void minAndMaxOfEqualAmountsKeepTheReceiver() {
        Money a = Money.myr("45");
        Money b = Money.myr("45.00");

        assertThat(a.min(b)).isSameAs(a);
        assertThat(a.max(b)).isSameAs(a);
    }

    @Test
    @DisplayName("Ringgit cannot be added to another currency.")
    void currenciesCannotBeMixed() {
        Money sgd = Money.of(BigDecimal.ONE, SGD);

        assertThatThrownBy(() -> Money.myr(1).plus(sgd)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.myr(1).minus(sgd)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.myr(1).compareTo(sgd)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MYR").hasMessageContaining("SGD");
    }

    @Test
    @DisplayName("An amount needs a value and a currency.")
    void missingValuesAreRefused() {
        assertThatThrownBy(() -> Money.of(null, Money.MYR)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.myr(1).plus(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.myr(1).times((BigDecimal) null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("RM39.38 prints as MYR 39.38.")
    void amountPrintsCurrencyAndPlainValue() {
        assertThat(Money.myr("39.38")).hasToString("MYR 39.38");
        assertThat(Money.myr("1E+2").toString()).isEqualTo("MYR 100");
    }
}
