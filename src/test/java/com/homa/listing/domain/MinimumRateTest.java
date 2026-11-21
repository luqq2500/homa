package com.homa.listing.domain;

import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HELPERS;
import static com.homa.fixtures.HomaFixtures.DISCOUNT_MIN_HOURS;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_DISCOUNT;
import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_RATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-11")
class MinimumRateTest {

    private static final HourlyRate FORTY = new HourlyRate(Money.myr(40));

    private static DiscountRule discountOf(String rate) {
        return DiscountRule.of(Percentage.of(rate), DISCOUNT_MIN_HOURS, DISCOUNT_MIN_HELPERS);
    }

    @Test
    @DisplayName("Kemas Home charges RM25, so its largest possible discount is 20% (RM25 × 0.8 = RM20)")
    void kemasHomesLargestDiscountIsTwentyPercent() {
        assertThat(KEMAS_HOME_RATE.afterDiscount(Percentage.of(20))).isEqualTo(Money.myr(20));
        assertThat(MinimumRate.HOMA.allows(KEMAS_HOME_RATE, discountOf("20"))).isTrue();
        assertThat(MinimumRate.HOMA.allows(KEMAS_HOME_RATE, discountOf("20.01"))).isFalse();
        assertThat(MinimumRate.HOMA.allows(KEMAS_HOME_RATE, discountOf("21"))).isFalse();
    }

    @Test
    @DisplayName("Its 10% discount gives RM22.50, which is allowed")
    void kemasHomesTenPercentDiscountIsAllowed() {
        assertThat(KEMAS_HOME_RATE.afterDiscount(KEMAS_HOME_DISCOUNT.rate())).isEqualTo(Money.myr("22.50"));
        assertThat(MinimumRate.HOMA.allows(KEMAS_HOME_RATE, KEMAS_HOME_DISCOUNT)).isTrue();
    }

    @Test
    @DisplayName("A provider charging RM40 could offer up to 50%, but not 51%")
    void aProviderChargingFortyCanOfferUpToHalfOff() {
        assertThat(MinimumRate.HOMA.allows(FORTY, discountOf("50"))).isTrue();
        assertThat(MinimumRate.HOMA.allows(FORTY, discountOf("51"))).isFalse();
    }

    @Test
    @DisplayName("With no discount, RM20 is the lowest rate: RM19.99 is under Homa's minimum")
    void withNoDiscountTheRateItselfMustBeAtLeastTwentyRinggit() {
        assertThat(MinimumRate.HOMA.perHelperHour()).isEqualTo(Money.myr(20));
        assertThat(MinimumRate.HOMA.allows(new HourlyRate(Money.myr(20)), DiscountRule.none())).isTrue();
        assertThat(MinimumRate.HOMA.allows(new HourlyRate(Money.myr("19.99")), DiscountRule.none())).isFalse();
        assertThatThrownBy(() -> new MinimumRate(null)).isInstanceOf(NullPointerException.class);
    }
}
