package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.DISCOUNT_RATE;
import static com.homa.fixtures.HomaFixtures.STANDARD_HELPERS;
import static com.homa.fixtures.HomaFixtures.STANDARD_HOURS;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_DISCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Percentage;
import com.homa.listing.domain.exception.InvalidDiscountException;
import com.homa.listing.domain.exception.ListingRuleViolationException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-4")
class DiscountRuleTest {

    @Test
    @DisplayName("Kemas Home's 10% applies to Aisyah's 4-hour, 2-helper booking, not to a 2-hour one.")
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

    @Test
    @DisplayName("A provider can set a discount of up to 99%: 99% is allowed, 99.01% and 100% are refused")
    void aDiscountRateIsAtMostNinetyNinePercent() {
        Duration threeHours = Duration.ofHours(3);

        assertThat(DiscountRule.of(Percentage.of(99), threeHours, 2).rate()).isEqualTo(Percentage.of(99));
        assertThatThrownBy(() -> DiscountRule.of(Percentage.of("99.01"), threeHours, 2))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("99%")
                .extracting(e -> ((ListingRuleViolationException) e).ruleId()).isEqualTo("OFR-4");
        assertThatThrownBy(() -> DiscountRule.of(Percentage.HUNDRED_PERCENT, threeHours, 2))
                .isInstanceOf(InvalidDiscountException.class);
    }

    @Test
    @Tag("OFR-11")
    @DisplayName("The minimum rate is checked against the discount's rate: Kemas Home's 10%, or nothing for no discount")
    void theRateCheckedAgainstTheMinimumIsTheDiscountsRate() {
        assertThat(KEMAS_HOME_DISCOUNT.rate()).isEqualTo(DISCOUNT_RATE);
        assertThat(DiscountRule.none().rate()).isEqualTo(Percentage.ZERO);
    }
}
