package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.DISCOUNT_RATE;
import static com.homa.fixtures.HomaFixtures.STANDARD_HELPERS;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_DISCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Percentage;
import com.homa.listing.domain.exception.InvalidDiscountException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-4")
class DiscountRuleTest {

    @Test
    @DisplayName("Kemas Home sets 10% for at least 3 hours and 2 helpers. Aisyah's 4-hour, 2-helper booking gets 10% off; "
            + "a 2-hour, 2-helper booking doesn't.")
    void aBookingThatMeetsTheMinimumHoursAndHelpersGetsTheDiscount() {
        assertThat(KEMAS_HOME_DISCOUNT.rateFor(STANDARD_HOURS, STANDARD_HELPERS)).isEqualTo(DISCOUNT_RATE);
        assertThat(KEMAS_HOME_DISCOUNT.rateFor(Duration.ofHours(2), 2)).isEqualTo(Percentage.ZERO);
    }

    @Test
    @DisplayName("Exactly 3 hours and 2 helpers qualifies; 2h 45m or 1 helper doesn't")
    void theMinimumsThemselvesQualify() {
        assertThat(KEMAS_HOME_DISCOUNT.rateFor(Duration.ofHours(3), 2)).isEqualTo(DISCOUNT_RATE);
        assertThat(KEMAS_HOME_DISCOUNT.rateFor(Duration.ofMinutes(165), 2)).isEqualTo(Percentage.ZERO);
        assertThat(KEMAS_HOME_DISCOUNT.rateFor(Duration.ofHours(4), 1)).isEqualTo(Percentage.ZERO);
    }

    @Test
    @DisplayName("By default there is no discount, whatever the booking")
    void noDiscountByDefault() {
        assertThat(DiscountRule.none().rateFor(Duration.ofHours(8), 10)).isEqualTo(Percentage.ZERO);
    }

    @Test
    @DisplayName("A discount needs a rate above zero, a minimum of hours and a minimum of at least 1 helper")
    void aDiscountNeedsARateHoursAndHelpers() {
        Duration threeHours = Duration.ofHours(3);
        assertThat(DiscountRule.of(Percentage.of(1), Duration.ofMinutes(1), 1).rateFor(Duration.ofMinutes(1), 1))
                .isEqualTo(Percentage.of(1));
        assertThatThrownBy(() -> DiscountRule.of(Percentage.ZERO, threeHours, 2)).isInstanceOf(InvalidDiscountException.class);
        assertThatThrownBy(() -> DiscountRule.of(DISCOUNT_RATE, Duration.ZERO, 2)).isInstanceOf(InvalidDiscountException.class);
        assertThatThrownBy(() -> DiscountRule.of(DISCOUNT_RATE, Duration.ofHours(-1), 2))
                .isInstanceOf(InvalidDiscountException.class);
        assertThatThrownBy(() -> DiscountRule.of(DISCOUNT_RATE, threeHours, 0)).isInstanceOf(InvalidDiscountException.class);
        assertThatThrownBy(() -> DiscountRule.of(null, threeHours, 2)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> DiscountRule.of(DISCOUNT_RATE, null, 2)).isInstanceOf(NullPointerException.class);
    }
}
