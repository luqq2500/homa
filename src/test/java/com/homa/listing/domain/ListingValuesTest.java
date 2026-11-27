package com.homa.listing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Each of Listing's rows in §13 Homa-set values, against its constant, so the rules page and the code can't drift. */
class ListingValuesTest {

    @Test
    @Tag("OFR-11")
    @DisplayName("Minimum rate: RM20 per helper hour, after discount")
    void theMinimumRateIsTwentyRinggitPerHelperHour() {
        assertThat(ListingValues.MINIMUM_RATE).isEqualTo(Money.myr(20));
        assertThat(MinimumRate.HOMA.perHelperHour()).isEqualTo(ListingValues.MINIMUM_RATE);
    }

    @Test
    @Tag("OFR-4")
    @DisplayName("Maximum discount: 99%")
    void theMaximumDiscountIsNinetyNinePercent() {
        assertThat(ListingValues.MAXIMUM_DISCOUNT).isEqualTo(Percentage.of(99));
    }

    @Test
    @Tag("OFR-5")
    @DisplayName("Default hours: minimum 1 hour, maximum 8 hours; default helpers: minimum 1, maximum = active helpers")
    void defaultBoundsAreOneToEightHoursAndOneHelperUpToTheActiveTeam() {
        assertThat(ListingValues.DEFAULT_MIN_HOURS).isEqualTo(Duration.ofHours(1));
        assertThat(ListingValues.DEFAULT_MAX_HOURS).isEqualTo(Duration.ofHours(8));
        assertThat(ListingValues.DEFAULT_MIN_HELPERS).isEqualTo(1);
        assertThat(BookingBounds.defaults().maxHelpers()).isEmpty();
    }

    @Test
    @Tag("RES-5")
    @DisplayName("Duration: minimum 1 hour, in 15-minute steps")
    void durationIsAtLeastOneHourInQuarterHourSteps() {
        assertThat(ListingValues.MINIMUM_DURATION).isEqualTo(Duration.ofHours(1));
        assertThat(ListingValues.DURATION_STEP).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    @Tag("OFR-6")
    @Tag("RES-4")
    @DisplayName("Minimum notice: default 2 hours; lowest 1 hour (Instant), 2 hours (Request)")
    void minimumNoticeDefaultsToTwoHoursWithAFloorPerPolicy() {
        assertThat(ListingValues.DEFAULT_MINIMUM_NOTICE).isEqualTo(Duration.ofHours(2));
        assertThat(ListingValues.INSTANT_NOTICE_FLOOR).isEqualTo(Duration.ofHours(1));
        assertThat(ListingValues.REQUEST_NOTICE_FLOOR).isEqualTo(Duration.ofHours(2));
        assertThat(MinimumNotice.DEFAULT.duration()).isEqualTo(ListingValues.DEFAULT_MINIMUM_NOTICE);
        assertThat(AcceptancePolicy.INSTANT.noticeFloor()).isEqualTo(ListingValues.INSTANT_NOTICE_FLOOR);
        assertThat(AcceptancePolicy.REQUEST.noticeFloor()).isEqualTo(ListingValues.REQUEST_NOTICE_FLOOR);
    }

    @Test
    @Tag("OFR-10")
    @Tag("RES-9")
    @DisplayName("Buffer between jobs: default 30 minutes")
    void theBufferBetweenJobsDefaultsToThirtyMinutes() {
        assertThat(ListingValues.DEFAULT_JOB_BUFFER).isEqualTo(Duration.ofMinutes(30));
        assertThat(JobBuffer.DEFAULT.duration()).isEqualTo(ListingValues.DEFAULT_JOB_BUFFER);
    }
}
