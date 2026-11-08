package com.homa.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @Nested
    class WritingAmounts {

        @ParameterizedTest(name = "\"{0}\" is {1} sen")
        @CsvSource({
            "230.00, 23000",
            "31.5,   3150",
            "0.01,   1",
            "180,    18000",
            "-69.00, -6900",
            "' 20 ', 2000"
        })
        void anAmountInRinggitIsHeldAsWholeSen(String ringgit, long sen) {
            assertThat(Money.parse(ringgit)).isEqualTo(Money.ofSen(sen));
        }

        @ParameterizedTest(name = "\"{0}\" is not an amount")
        @ValueSource(strings = {"0.001", "RM20", "", "twenty"})
        void anAmountFinerThanOneSenOrNotANumberIsRejected(String text) {
            assertThatThrownBy(() -> Money.parse(text))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(text);
        }

        @Test
        void wholeRinggitAreHundredsOfSen() {
            assertThat(Money.ofRinggit(230)).isEqualTo(Money.ofSen(23_000));
        }

        @Test
        void zeroIsNoMoney() {
            assertThat(Money.ZERO.sen()).isZero();
            assertThat(Money.ZERO.isZero()).isTrue();
            assertThat(Money.ofSen(1).isZero()).isFalse();
        }

        @ParameterizedTest(name = "{0} sen is shown as {1}")
        @CsvSource({
            "23000, RM230.00",
            "3150,  RM31.50",
            "5,     RM0.05",
            "0,     RM0.00",
            "-6900, -RM69.00"
        })
        void moneyIsShownInRinggitWithTwoDecimals(long sen, String shown) {
            assertThat(Money.ofSen(sen)).hasToString(shown);
        }
    }

    @Nested
    class Arithmetic {

        @Test
        void amountsAddUp() {
            assertThat(Money.parse("180").plus(Money.parse("20"))).isEqualTo(Money.parse("200"));
        }

        @Test
        void subtractingCanGoBelowZero() {
            assertThat(Money.parse("230").minus(Money.parse("299"))).isEqualTo(Money.parse("-69"));
        }

        @Test
        void aRateTimesACountMultiplies() {
            assertThat(Money.parse("30").times(6)).isEqualTo(Money.parse("180"));
        }

        @Test
        void overflowIsAnErrorNotAWrapAround() {
            Money most = Money.ofSen(Long.MAX_VALUE);
            assertThatThrownBy(() -> most.plus(Money.ofSen(1))).isInstanceOf(ArithmeticException.class);
            assertThatThrownBy(() -> Money.ofSen(Long.MIN_VALUE).minus(Money.ofSen(1)))
                    .isInstanceOf(ArithmeticException.class);
            assertThatThrownBy(() -> most.times(2)).isInstanceOf(ArithmeticException.class);
            assertThatThrownBy(() -> Money.ofRinggit(Long.MAX_VALUE)).isInstanceOf(ArithmeticException.class);
        }

        @Test
        void theSmallerOfTwoAmountsIsTheMinimum() {
            Money forty = Money.parse("40");
            Money fiftyFour = Money.parse("54");
            assertThat(forty.min(fiftyFour)).isSameAs(forty);
            assertThat(fiftyFour.min(forty)).isSameAs(forty);
            assertThat(forty.min(Money.parse("40.00"))).isSameAs(forty);
        }
    }

    @Nested
    class Comparing {

        @Test
        void aNegativeAmountIsBelowZero() {
            assertThat(Money.ofSen(-1).isNegative()).isTrue();
            assertThat(Money.ZERO.isNegative()).isFalse();
            assertThat(Money.ofSen(1).isNegative()).isFalse();
        }

        @Test
        void greaterAndLessCompareBySen() {
            Money small = Money.ofSen(100);
            Money large = Money.ofSen(101);

            assertThat(large.isGreaterThan(small)).isTrue();
            assertThat(small.isGreaterThan(large)).isFalse();
            assertThat(small.isGreaterThan(Money.ofSen(100))).isFalse();

            assertThat(small.isLessThan(large)).isTrue();
            assertThat(large.isLessThan(small)).isFalse();
            assertThat(small.isLessThan(Money.ofSen(100))).isFalse();
        }

        @Test
        void amountsSortFromSmallestToLargest() {
            assertThat(Money.ofSen(1).compareTo(Money.ofSen(2))).isNegative();
            assertThat(Money.ofSen(2).compareTo(Money.ofSen(1))).isPositive();
            assertThat(Money.ofSen(2).compareTo(Money.ofSen(2))).isZero();
        }

        @Test
        void equalAmountsAreTheSameMoney() {
            assertThat(Money.parse("31.50")).isEqualTo(Money.ofSen(3150)).hasSameHashCodeAs(Money.ofSen(3150));
        }
    }
}
