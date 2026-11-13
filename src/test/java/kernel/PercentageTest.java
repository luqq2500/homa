package kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PercentageTest {

    @Test
    void aPercentageIsFromZeroToAHundred() {
        assertThat(Percentage.of(0)).isEqualTo(Percentage.ZERO);
        assertThat(Percentage.of(100).percent()).isEqualTo(100);
        assertThatThrownBy(() -> Percentage.of(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Percentage.of(101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aShareOfAnAmountIsWorkedOutToTheSen() {
        assertThat(Percentage.of(15).of(Money.ofRinggit(360))).isEqualTo(Money.ofRinggit(54));
    }

    @Test
    void aShareIsRoundedHalfUpToTheNearestSen() {
        assertThat(Percentage.of(15).of(Money.ofSen(110))).isEqualTo(Money.ofSen(17));
        assertThat(Percentage.of(15).of(Money.ofSen(103))).isEqualTo(Money.ofSen(15));
        assertThat(Percentage.of(15).of(Money.ofSen(109))).isEqualTo(Money.ofSen(16));
    }

    @Test
    void aShareCanBeRoundedDownInstead() {
        assertThat(Percentage.of(50).roundedDownOf(Money.ofSen(101))).isEqualTo(Money.ofSen(50));
        assertThat(Percentage.of(15).roundedDownOf(Money.ofSen(110))).isEqualTo(Money.ofSen(16));
        assertThat(Percentage.of(50).roundedDownOf(Money.ofSen(100))).isEqualTo(Money.ofSen(50));
    }

    @Test
    void onlyNoneIsZero() {
        assertThat(Percentage.ZERO.isZero()).isTrue();
        assertThat(Percentage.of(1).isZero()).isFalse();
    }

    @Test
    void aLargerPercentageIsMoreThanASmallerOne() {
        assertThat(Percentage.of(30).isMoreThan(Percentage.of(20))).isTrue();
        assertThat(Percentage.of(20).isMoreThan(Percentage.of(20))).isFalse();
        assertThat(Percentage.of(10).isMoreThan(Percentage.of(20))).isFalse();
    }

    @Test
    void percentagesAreOrderedBySize() {
        assertThat(Percentage.of(10)).isLessThan(Percentage.of(20));
        assertThat(Percentage.of(20)).isGreaterThan(Percentage.of(10));
        assertThat(Percentage.of(20)).isEqualByComparingTo(Percentage.of(20));
    }
}
