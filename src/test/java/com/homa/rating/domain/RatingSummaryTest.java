package com.homa.rating.domain;

import static com.homa.rating.domain.RatingFixtures.AISYAH_PARTY;
import static com.homa.rating.domain.RatingFixtures.AISYAH_RATES_AT;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RatingSummaryTest {

    private static Rating byACustomer(int stars, Map<RatingCategory, Stars> categories) {
        return Rating.by(RatingId.generate(), AISYAH_PARTY, new Stars(stars), null, categories, List.of(),
                AISYAH_RATES_AT);
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("Kemas Home has no revealed ratings yet: there is a count of 0 and nothing to average.")
    void aProviderWithNoRatingsHasNothingToAverage() {
        RatingSummary summary = RatingSummary.of(List.of());

        assertThat(summary.ratingCount()).isZero();
        assertThat(summary.averageStars()).isEmpty();
        assertThat(summary.averageOf(RatingCategory.PUNCTUALITY)).isEmpty();
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("Other customers see Aisyah's 4 stars on Kemas Home's page: one rating, average 4.0.")
    void oneRatingIsItsOwnAverage() {
        RatingSummary summary = RatingSummary.of(List.of(byACustomer(4, Map.of())));

        assertThat(summary.ratingCount()).isEqualTo(1);
        assertThat(summary.averageStars()).contains(new BigDecimal("4.0"));
        assertThat(summary.averageOf(RatingCategory.QUALITY_OF_WORK)).isEmpty();
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("Ratings of 5, 3 and 3 average 3.7 to one decimal place, rounded half up.")
    void theAverageIsShownToOneDecimalPlace() {
        RatingSummary summary = RatingSummary.of(List.of(byACustomer(5, Map.of()), byACustomer(3, Map.of()),
                byACustomer(3, Map.of())));

        assertThat(summary.ratingCount()).isEqualTo(3);
        assertThat(summary.averageStars()).contains(new BigDecimal("3.7"));
    }

    private static List<Rating> fiveFivesAndFifteenFours() {
        return java.util.stream.IntStream.range(0, 20).mapToObj(i -> byACustomer(i < 5 ? 5 : 4, Map.of())).toList();
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("Ratings of 4 and 5 average 4.5; of 4, 4 and 5 average 4.3; five 5s and fifteen 4s are exactly 4.25, "
            + "shown 4.3; of 1 and 2 average 1.5.")
    void halfUpAtTheBoundary() {
        assertThat(RatingSummary.of(List.of(byACustomer(4, Map.of()), byACustomer(5, Map.of()))).averageStars())
                .contains(new BigDecimal("4.5"));
        assertThat(RatingSummary.of(List.of(byACustomer(4, Map.of()), byACustomer(4, Map.of()),
                byACustomer(5, Map.of()))).averageStars()).contains(new BigDecimal("4.3"));
        assertThat(RatingSummary.of(fiveFivesAndFifteenFours()).averageStars()).contains(new BigDecimal("4.3"));
        assertThat(RatingSummary.of(List.of(byACustomer(1, Map.of()), byACustomer(2, Map.of()))).averageStars())
                .contains(new BigDecimal("1.5"));
    }

    @Test
    @Tag("RTG-7")
    @DisplayName("Aisyah gives 5, 4 and 5 and another customer gives Punctuality 2 only: a category averages over the "
            + "ratings that gave it.")
    void aCategoryAveragesOverTheRatingsThatGaveIt() {
        Rating aisyahs = byACustomer(5, Map.of(RatingCategory.QUALITY_OF_WORK, new Stars(5),
                RatingCategory.PUNCTUALITY, new Stars(4), RatingCategory.PROFESSIONALISM, new Stars(5)));
        Rating mei = byACustomer(3, Map.of(RatingCategory.PUNCTUALITY, new Stars(2)));

        RatingSummary summary = RatingSummary.of(List.of(aisyahs, mei));

        assertThat(summary.averageOf(RatingCategory.QUALITY_OF_WORK)).contains(new BigDecimal("5.0"));
        assertThat(summary.averageOf(RatingCategory.PUNCTUALITY)).contains(new BigDecimal("3.0"));
        assertThat(summary.averageOf(RatingCategory.PROFESSIONALISM)).contains(new BigDecimal("5.0"));
        assertThat(summary.averageStars()).contains(new BigDecimal("4.0"));
    }

    @Test
    @Tag("RTG-8")
    @DisplayName("A customer's ratings, written by providers, average on the provider's categories.")
    void aCustomersRatingsAverageOnTheProvidersCategories() {
        Rating kemasHomes = Rating.by(RatingId.generate(), RatingFixtures.KEMAS_HOME_PARTY, new Stars(4), null,
                Map.of(RatingCategory.ACCESS_AND_READINESS, new Stars(3)), List.of(), AISYAH_RATES_AT);

        RatingSummary summary = RatingSummary.of(List.of(kemasHomes));

        assertThat(summary.averageOf(RatingCategory.ACCESS_AND_READINESS)).contains(new BigDecimal("3.0"));
        assertThat(summary.averageOf(RatingCategory.RESPECT)).isEmpty();
    }
}
