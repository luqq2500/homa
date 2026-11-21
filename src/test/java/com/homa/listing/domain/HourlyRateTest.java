package com.homa.listing.domain;

import static com.homa.listing.domain.ListingFixtures.KEMAS_HOME_RATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.listing.domain.exception.InvalidHourlyRateException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("OFR-2")
class HourlyRateTest {

    @Test
    @DisplayName("Kemas Home sets RM25 per helper per hour. A 2-hour job with 2 helpers is priced 25 × 2 × 2 = RM100 before travel.")
    void aTwoHourJobWithTwoHelpersIsPricedPerHelperPerHour() {
        assertThat(KEMAS_HOME_RATE.priceFor(Duration.ofHours(2), 2)).isEqualTo(Money.myr(100));
    }

    @Test
    @DisplayName("Quarter hours are priced at their share of the hourly rate")
    void quarterHoursArePricedAtTheirShareOfTheRate() {
        assertThat(KEMAS_HOME_RATE.priceFor(Duration.ofMinutes(90), 1)).isEqualTo(Money.myr("37.5"));
        assertThat(KEMAS_HOME_RATE.priceFor(Duration.ofMinutes(75), 3)).isEqualTo(Money.myr("93.75"));
    }

    @Test
    @DisplayName("A rate of zero or less is refused")
    void aRateMustBeMoreThanZero() {
        assertThat(new HourlyRate(Money.myr("0.01")).perHelperPerHour()).isEqualTo(Money.myr("0.01"));
        assertThatThrownBy(() -> new HourlyRate(Money.myr(0))).isInstanceOf(InvalidHourlyRateException.class);
        assertThatThrownBy(() -> new HourlyRate(Money.myr(-25))).isInstanceOf(InvalidHourlyRateException.class);
        assertThatThrownBy(() -> new HourlyRate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("OFR-11")
    @DisplayName("Kemas Home's RM25 after its 10% discount is RM22.50 per helper hour; with no discount it stays RM25")
    void theRateAfterADiscountIsTheRateLessTheDiscount() {
        assertThat(KEMAS_HOME_RATE.afterDiscount(Percentage.of(10))).isEqualTo(Money.myr("22.50"));
        assertThat(KEMAS_HOME_RATE.afterDiscount(Percentage.ZERO)).isEqualTo(Money.myr(25));
    }
}
