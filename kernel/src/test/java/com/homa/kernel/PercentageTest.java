package com.homa.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PercentageTest {

    @Nested
    class Range {

        @ParameterizedTest(name = "{0} basis points is a valid share")
        @ValueSource(ints = {0, 1, 1500, 9999, 10000})
        void aShareFromNothingToEverythingIsValid(int basisPoints) {
            assertThat(Percentage.ofBasisPoints(basisPoints).basisPoints()).isEqualTo(basisPoints);
        }

        @ParameterizedTest(name = "{0} basis points is rejected")
        @ValueSource(ints = {-1, 10001})
        void aShareBelowNothingOrAboveEverythingIsRejected(int basisPoints) {
            assertThatThrownBy(() -> Percentage.ofBasisPoints(basisPoints))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(basisPoints));
        }

        @Test
        void wholePercentsAreHundredsOfBasisPoints() {
            assertThat(Percentage.of(15)).isEqualTo(Percentage.ofBasisPoints(1500));
            assertThat(Percentage.of(100)).isEqualTo(Percentage.ofBasisPoints(10000));
            assertThat(Percentage.ZERO).isEqualTo(Percentage.of(0));
        }

        @Test
        void percentsOutsideTheRangeAreRejected() {
            assertThatThrownBy(() -> Percentage.of(101)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Percentage.of(-1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Percentage.of(Integer.MAX_VALUE)).isInstanceOf(ArithmeticException.class);
        }
    }

    @Nested
    class ShareOfMoney {

        @ParameterizedTest(name = "{0} of {1} is {2}")
        @CsvSource({
            "15%,  200.00, 30.00",
            "15%,  210.00, 31.50",
            "10%,  180.00, 18.00",
            "15%,  18.00,  2.70",
            "30%,  180.00, 54.00",
            "50%,  80.00,  40.00",
            "0%,   230.00, 0.00",
            "100%, 230.00, 230.00"
        })
        void aShareOfAnAmountIsExactWhenItFallsOnASen(String share, String amount, String expected) {
            assertThat(percent(share).of(Money.parse(amount))).isEqualTo(Money.parse(expected));
        }

        @ParameterizedTest(name = "{0} of {1} sen rounds to {2} sen")
        @CsvSource({
            "1500, 3,  0",
            "1500, 10, 2",
            "5000, 1,  1",
            "5000, 3,  2",
            "1500, 9,  1",
            "5000, -1, -1",
            "5000, -3, -2"
        })
        void aShareRoundsHalfUpToTheNearestSen(int basisPoints, long sen, long expectedSen) {
            assertThat(Percentage.ofBasisPoints(basisPoints).of(Money.ofSen(sen))).isEqualTo(Money.ofSen(expectedSen));
        }

        private Percentage percent(String text) {
            return Percentage.of(Integer.parseInt(text.replace("%", "")));
        }
    }

    @Nested
    class Comparing {

        @Test
        void aLargerShareIsGreater() {
            assertThat(Percentage.of(30).isGreaterThan(Percentage.of(20))).isTrue();
            assertThat(Percentage.of(20).isGreaterThan(Percentage.of(30))).isFalse();
            assertThat(Percentage.of(30).isGreaterThan(Percentage.of(30))).isFalse();
        }

        @Test
        void sharesSortFromSmallestToLargest() {
            assertThat(Percentage.of(10).compareTo(Percentage.of(20))).isNegative();
            assertThat(Percentage.of(20).compareTo(Percentage.of(10))).isPositive();
            assertThat(Percentage.of(20).compareTo(Percentage.of(20))).isZero();
        }

        @ParameterizedTest(name = "{0} basis points is shown as {1}")
        @CsvSource({"1500, 15%", "1250, 12.5%", "0, 0%", "10000, 100%", "1, 0.01%"})
        void aShareIsShownAsAPercent(int basisPoints, String shown) {
            assertThat(Percentage.ofBasisPoints(basisPoints)).hasToString(shown);
        }
    }
}
