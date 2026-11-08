package feedback.domain;

import static feedback.domain.FeedbackBuilders.LAUNCH_LIMITS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feedback.domain.exception.FeedbackDetailRequiredException;
import feedback.domain.exception.NoRatingToWithdrawException;
import feedback.domain.exception.RatingDoesNotFitSummaryException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Feature: Rating summaries. */
class RatingSummaryAveragingTest {

    private static RatingSummary aProviderRated(int... stars) {
        RatingSummary summary = RatingSummary.forProvider("P1");
        for (int given : stars) {
            summary.record(Stars.of(given), Map.of(), Set.of());
        }
        return summary;
    }

    private static void compliment(RatingSummary helper, int times, Compliment compliment) {
        for (int i = 0; i < times; i++) {
            helper.record(Stars.of(5), Map.of(), Set.of(compliment));
        }
    }

    @Nested
    class Averages {

        @ParameterizedTest(name = "with {0} ratings the average is shown: {1}")
        @CsvSource({"2, false", "3, true", "4, true"})
        void averageStarsAreShownOnlyAfterTheMinimumNumberOfRatings(int count, boolean shown) {
            RatingSummary summary = RatingSummary.forProvider("P1");
            for (int i = 0; i < count; i++) {
                summary.record(Stars.of(4), Map.of(), Set.of());
            }

            assertThat(summary.isShown(LAUNCH_LIMITS)).isEqualTo(shown);
            assertThat(summary.ratingCount()).isEqualTo(count);
        }

        @Test
        void theAverageIsRoundedToOneDecimalPlace() {
            assertThat(aProviderRated(5, 4, 4).averageStars()).isEqualTo(new BigDecimal("4.3"));
        }

        @Test
        void theAverageRoundsHalfUp() {
            assertThat(aProviderRated(5, 4, 5, 4).averageStars()).isEqualTo(new BigDecimal("4.5"));
            assertThat(aProviderRated(5, 5, 5, 4, 4, 4, 4, 4).averageStars()).isEqualTo(new BigDecimal("4.4"));
            assertThat(aProviderRated(5, 5, 4, 4, 4, 4, 4, 4).averageStars()).isEqualTo(new BigDecimal("4.3"));
        }

        @Test
        void aSummaryWithNoRatingsAveragesZeroAndIsNotShown() {
            RatingSummary summary = RatingSummary.forProvider("P1");

            assertThat(summary.averageStars()).isEqualTo(new BigDecimal("0.0"));
            assertThat(summary.isShown(LAUNCH_LIMITS)).isFalse();
            assertThat(summary.ratingCount()).isZero();
        }

        @Test
        void withdrawingAHiddenRatingRemovesItFromTheAverage() {
            RatingSummary summary = aProviderRated(5, 4, 1);

            summary.withdraw(Stars.of(1), Map.of(), Set.of());

            assertThat(summary.averageStars()).isEqualTo(new BigDecimal("4.5"));
            assertThat(summary.ratingCount()).isEqualTo(2);
        }

        @Test
        void aSummaryCannotWithdrawMoreRatingsThanItHas() {
            RatingSummary helper = RatingSummary.forHelper("aina");

            assertThatThrownBy(() -> helper.withdraw(Stars.of(5), Map.of(), Set.of()))
                    .isInstanceOf(NoRatingToWithdrawException.class)
                    .hasMessageContaining("helper aina");
        }
    }

    @Nested
    class Categories {

        @Test
        void categoryAveragesCountOnlyTheRatingsThatGaveThatCategory() {
            RatingSummary summary = RatingSummary.forProvider("P1");
            summary.record(Stars.of(4), Map.of(ProviderCategory.PUNCTUALITY, Stars.of(5)), Set.of());
            summary.record(Stars.of(4), Map.of(ProviderCategory.PUNCTUALITY, Stars.of(3)), Set.of());
            summary.record(Stars.of(4), Map.of(), Set.of());
            summary.record(Stars.of(4), Map.of(ProviderCategory.QUALITY_OF_WORK, Stars.of(2)), Set.of());

            assertThat(summary.categoryAverage(ProviderCategory.PUNCTUALITY)).contains(new BigDecimal("4.0"));
            assertThat(summary.categoryAverage(ProviderCategory.QUALITY_OF_WORK)).contains(new BigDecimal("2.0"));
            assertThat(summary.categoryAverage(ProviderCategory.COMMUNICATION)).isEmpty();
        }

        @Test
        void withdrawingARatingTakesBackItsCategoryStars() {
            RatingSummary summary = RatingSummary.forProvider("P1");
            summary.record(Stars.of(4), Map.of(ProviderCategory.PUNCTUALITY, Stars.of(5)), Set.of());
            summary.record(Stars.of(4), Map.of(ProviderCategory.PUNCTUALITY, Stars.of(1), ProviderCategory.COMMUNICATION, Stars.of(2)), Set.of());

            summary.withdraw(Stars.of(4), Map.of(ProviderCategory.PUNCTUALITY, Stars.of(1), ProviderCategory.COMMUNICATION, Stars.of(2)), Set.of());

            assertThat(summary.categoryAverage(ProviderCategory.PUNCTUALITY)).contains(new BigDecimal("5.0"));
            assertThat(summary.categoryAverage(ProviderCategory.COMMUNICATION)).isEmpty();
        }

        @Test
        void aCategoryThatWasNeverRatedCannotBeWithdrawn() {
            RatingSummary summary = aProviderRated(4);

            assertThatThrownBy(() -> summary.withdraw(Stars.of(4), Map.of(ProviderCategory.PUNCTUALITY, Stars.of(3)), Set.of()))
                    .isInstanceOf(NoRatingToWithdrawException.class);
            assertThat(summary.ratingCount()).isEqualTo(1);
        }

        @Test
        void helpersHaveNoCategoryRatings() {
            assertThatThrownBy(() -> RatingSummary.forHelper("aina")
                            .record(Stars.of(5), Map.of(ProviderCategory.PUNCTUALITY, Stars.of(5)), Set.of()))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class)
                    .hasMessage("Category ratings are for providers, not helpers");
        }
    }

    @Nested
    class Compliments {

        @Test
        void theMostGivenComplimentsComeFirst() {
            RatingSummary helper = RatingSummary.forHelper("aina");
            compliment(helper, 1, Compliment.COOPERATIVE);
            compliment(helper, 3, Compliment.FRIENDLY);
            compliment(helper, 2, Compliment.THOROUGH);

            assertThat(helper.topCompliments(2)).containsExactly(Compliment.FRIENDLY, Compliment.THOROUGH);
            assertThat(helper.topCompliments(5)).containsExactly(Compliment.FRIENDLY, Compliment.THOROUGH, Compliment.COOPERATIVE);
            assertThat(helper.complimentCount(Compliment.FRIENDLY)).isEqualTo(3);
            assertThat(helper.complimentCount(Compliment.HARDWORKING)).isZero();
        }

        @Test
        void complimentsGivenEquallyOftenKeepTheirListedOrder() {
            RatingSummary helper = RatingSummary.forHelper("aina");
            compliment(helper, 2, Compliment.THOROUGH);
            compliment(helper, 2, Compliment.FRIENDLY);

            assertThat(helper.topCompliments(2)).containsExactly(Compliment.FRIENDLY, Compliment.THOROUGH);
        }

        @Test
        void withdrawingARatingTakesBackItsCompliments() {
            RatingSummary helper = RatingSummary.forHelper("aina");
            helper.record(Stars.of(5), Map.of(), Set.of(Compliment.FRIENDLY, Compliment.THOROUGH));
            helper.record(Stars.of(4), Map.of(), Set.of(Compliment.FRIENDLY));

            helper.withdraw(Stars.of(5), Map.of(), Set.of(Compliment.FRIENDLY, Compliment.THOROUGH));

            assertThat(helper.complimentCount(Compliment.FRIENDLY)).isEqualTo(1);
            assertThat(helper.complimentCount(Compliment.THOROUGH)).isZero();
            assertThat(helper.topCompliments(5)).containsExactly(Compliment.FRIENDLY);
            assertThat(helper.averageStars()).isEqualTo(new BigDecimal("4.0"));
        }

        @Test
        void aComplimentThatWasNeverGivenCannotBeWithdrawn() {
            RatingSummary helper = RatingSummary.forHelper("aina");
            helper.record(Stars.of(5), Map.of(), Set.of(Compliment.FRIENDLY));

            assertThatThrownBy(() -> helper.withdraw(Stars.of(5), Map.of(), Set.of(Compliment.THOROUGH)))
                    .isInstanceOf(NoRatingToWithdrawException.class);
        }

        @Test
        void aRatingOfTheWrongKindCannotBeWithdrawnEither() {
            RatingSummary helper = RatingSummary.forHelper("aina");
            helper.record(Stars.of(5), Map.of(), Set.of());
            RatingSummary provider = aProviderRated(4);

            assertThatThrownBy(() -> helper.withdraw(Stars.of(5), Map.of(ProviderCategory.PUNCTUALITY, Stars.of(5)), Set.of()))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class);
            assertThatThrownBy(() -> provider.withdraw(Stars.of(4), Map.of(), Set.of(Compliment.FRIENDLY)))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class);
            assertThat(helper.ratingCount()).isEqualTo(1);
            assertThat(provider.ratingCount()).isEqualTo(1);
        }

        @Test
        void providersHaveNoCompliments() {
            assertThatThrownBy(() -> RatingSummary.forProvider("P1").record(Stars.of(5), Map.of(), Set.of(Compliment.FRIENDLY)))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class)
                    .hasMessage("Compliments are for helpers, not providers");
        }
    }

    @Test
    void aSummaryIsAboutOneProviderOrOneHelper() {
        assertThat(RatingSummary.forProvider("P1").subject()).isEqualTo(RatingSummary.Subject.PROVIDER);
        assertThat(RatingSummary.forProvider("P1").subjectId()).isEqualTo("P1");
        assertThat(RatingSummary.forHelper("aina").subject()).isEqualTo(RatingSummary.Subject.HELPER);
        assertThatThrownBy(() -> RatingSummary.forProvider(" ")).isInstanceOf(FeedbackDetailRequiredException.class);
        assertThatThrownBy(() -> RatingSummary.forHelper(null)).isInstanceOf(FeedbackDetailRequiredException.class);
    }
}
