package kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void oneRinggitIsAHundredSen() {
        assertThat(Money.ofRinggit(36)).isEqualTo(Money.ofSen(3600));
    }

    @Test
    void moneyIsNeverNegative() {
        assertThatThrownBy(() -> Money.ofSen(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Money.ofSen(0)).isEqualTo(Money.ZERO);
    }

    @Test
    void amountsAddUp() {
        assertThat(Money.ofSen(1050).plus(Money.ofSen(250))).isEqualTo(Money.ofSen(1300));
    }

    @Test
    void anAmountCanBeTakenAway() {
        assertThat(Money.ofSen(1050).minus(Money.ofSen(250))).isEqualTo(Money.ofSen(800));
        assertThat(Money.ofSen(1050).minus(Money.ofSen(1050))).isEqualTo(Money.ZERO);
    }

    @Test
    void takingAwayMoreThanThereIsFails() {
        assertThatThrownBy(() -> Money.ofSen(100).minus(Money.ofSen(101)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAmountIsMultipliedByAWholeNumber() {
        assertThat(Money.ofRinggit(30).times(6)).isEqualTo(Money.ofRinggit(180));
    }

    @Test
    void onlyNothingIsZero() {
        assertThat(Money.ZERO.isZero()).isTrue();
        assertThat(Money.ofSen(1).isZero()).isFalse();
    }

    @Test
    void aLargerAmountIsMoreThanASmallerOne() {
        assertThat(Money.ofSen(2).isMoreThan(Money.ofSen(1))).isTrue();
        assertThat(Money.ofSen(1).isMoreThan(Money.ofSen(1))).isFalse();
        assertThat(Money.ofSen(1).isMoreThan(Money.ofSen(2))).isFalse();
    }

    @Test
    void theSmallerOfTwoAmountsIsTheMinimum() {
        assertThat(Money.ofSen(5).min(Money.ofSen(3))).isEqualTo(Money.ofSen(3));
        assertThat(Money.ofSen(3).min(Money.ofSen(5))).isEqualTo(Money.ofSen(3));
    }

    @Test
    void amountsAreOrderedBySize() {
        assertThat(Money.ofSen(1)).isLessThan(Money.ofSen(2));
        assertThat(Money.ofSen(2)).isGreaterThan(Money.ofSen(1));
        assertThat(Money.ofSen(2)).isEqualByComparingTo(Money.ofSen(2));
    }
}
