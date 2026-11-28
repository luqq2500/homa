package com.homa.rating.domain;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RatingValuesTest {

    private static final RatingValues.Version FIRST = RatingValues.current();
    /** A made-up later change: from Tuesday 1 December 2026, a 10-day Rating Window. */
    private static final Instant CHANGED_FROM = at(2026, 12, 1, 0, 0);
    private static final RatingValues.Version CHANGED = new RatingValues.Version(CHANGED_FROM, Duration.ofDays(10));

    @Test
    @Tag("RTG-2")
    @DisplayName("Homa-set values: the Rating Window closes 7 days after the Protection Window ends.")
    void theRatingWindowIsSevenDaysAfterTheProtectionWindowEnds() {
        RatingValues.Version current = RatingValues.current();

        assertThat(current.effectiveFrom()).isEqualTo(Instant.EPOCH);
        assertThat(current.ratingWindow()).isEqualTo(Duration.ofDays(7));
        assertThat(RatingValues.asOf(REQUESTED_AT)).isEqualTo(current);
        assertThat(RatingValues.homa().inForceAt(REQUESTED_AT)).isEqualTo(current);
        assertThat(current.closesAfter(RatingFixtures.PROTECTION_WINDOW_END)).isEqualTo(RatingFixtures.WINDOW_CLOSES_AT);
    }

    @Test
    @Tag("RTG-4")
    @Tag("RTG-7")
    @Tag("RTG-8")
    @Tag("RTG-10")
    @DisplayName("Homa-set values: 1 to 5 stars, 3 categories per side, reliability over 12 months.")
    void theAccountValuesAreTheStarsTheCategoriesPerSideAndTheReliabilityPeriod() {
        assertThat(RatingValues.MINIMUM_STARS).isEqualTo(1);
        assertThat(RatingValues.MAXIMUM_STARS).isEqualTo(5);
        assertThat(RatingValues.CATEGORIES_PER_SIDE).isEqualTo(3);
        assertThat(RatingValues.RELIABILITY_PERIOD_MONTHS).isEqualTo(12);
        assertThat(RatingCategory.of(PartyRole.CUSTOMER)).hasSize(RatingValues.CATEGORIES_PER_SIDE);
        assertThat(RatingCategory.of(PartyRole.PROVIDER)).hasSize(RatingValues.CATEGORIES_PER_SIDE);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Homa changes its values from Tuesday: Aisyah's reservation, Requested on Monday 20:00, keeps the "
            + "7-day window; a reservation Requested from Tuesday 00:00 gets the new one.")
    void reservationKeepsTheVersionInForceWhenItWasRequested() {
        RatingValues values = new RatingValues(List.of(FIRST, CHANGED));

        assertThat(values.inForceAt(REQUESTED_AT)).isEqualTo(FIRST);
        assertThat(values.inForceAt(CHANGED_FROM.minusNanos(1))).isEqualTo(FIRST);
        assertThat(values.inForceAt(CHANGED_FROM)).isEqualTo(CHANGED);
        assertThat(values.inForceAt(at(2027, 1, 1, 0, 0))).isEqualTo(CHANGED);
        assertThat(values.newest()).isEqualTo(CHANGED);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("Rating's values start with a version in force from the epoch, each later one strictly after.")
    void versionsStartAtTheEpochAndFollowInOrder() {
        RatingValues.Version notAtEpoch = new RatingValues.Version(CHANGED_FROM, Duration.ofDays(7));
        RatingValues.Version sameStart = new RatingValues.Version(Instant.EPOCH, Duration.ofDays(10));

        assertThatThrownBy(() -> new RatingValues(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RatingValues(List.of(notAtEpoch)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("epoch");
        assertThatThrownBy(() -> new RatingValues(List.of(FIRST, sameStart)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1970-01-01T00:00:00Z");
        assertThatThrownBy(() -> new RatingValues(List.of(FIRST, CHANGED, FIRST)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new RatingValues(List.of(FIRST, CHANGED)).newest()).isEqualTo(CHANGED);
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("The Rating Window is a whole number of days, counted on the Kuala Lumpur calendar.")
    void theWindowIsAWholeNumberOfDays() {
        assertThatThrownBy(() -> new RatingValues.Version(Instant.EPOCH, Duration.ofHours(36)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("whole number of days");
        assertThatThrownBy(() -> new RatingValues.Version(Instant.EPOCH, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RatingValues.Version(Instant.EPOCH, Duration.ofDays(-7)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RatingValues.Version(null, Duration.ofDays(7)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RatingValues.Version(Instant.EPOCH, null))
                .isInstanceOf(NullPointerException.class);
        assertThat(CHANGED.closesAfter(RatingFixtures.PROTECTION_WINDOW_END)).isEqualTo(at(2026, 12, 16, 14, 0));
    }
}
