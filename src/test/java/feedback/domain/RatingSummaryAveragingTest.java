package feedback.domain;

import static feedback.domain.FeedbackBuilders.LAUNCH_LIMITS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feedback.domain.exception.FeedbackDetailRequiredException;
import feedback.domain.exception.NoRatingToWithdrawException;
import feedback.domain.exception.RatingDoesNotFitSummaryException;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Feature: Rating summaries. */
class RatingSummaryAveragingTest {

    private static final String PUNCTUALITY = "PUNCTUALITY";
    private static final String COMMUNICATION = "COMMUNICATION";

    private static RatingSummary aProviderRated(int... overall) {
        RatingSummary summary = RatingSummary.forProvider("P1");
        for (int given : overall) {
            summary.record(Stars.of(given), Map.of());
        }
        return summary;
    }

    @Nested
    class TheOverallAverage {

        @ParameterizedTest(name = "with {0} ratings the overall average is shown: {1}")
        @CsvSource({"2, false", "3, true", "4, true"})
        void theOverallAverageIsShownOnlyAfterTheMinimumNumberOfRatings(int count, boolean shown) {
            RatingSummary summary = RatingSummary.forProvider("P1");
            for (int i = 0; i < count; i++) {
                summary.record(Stars.of(4), Map.of());
            }

            assertThat(summary.isOverallShown(LAUNCH_LIMITS)).isEqualTo(shown);
            assertThat(summary.ratingCount()).isEqualTo(count);
        }

        @Test
        void theAverageIsRoundedToOneDecimalPlace() {
            assertThat(aProviderRated(5, 4, 4).overallAverage()).isEqualTo(new BigDecimal("4.3"));
        }

        @Test
        void theAverageRoundsHalfUp() {
            assertThat(aProviderRated(5, 4, 5, 4).overallAverage()).isEqualTo(new BigDecimal("4.5"));
            assertThat(aProviderRated(5, 5, 5, 4, 4, 4, 4, 4).overallAverage()).isEqualTo(new BigDecimal("4.4"));
            assertThat(aProviderRated(5, 5, 4, 4, 4, 4, 4, 4).overallAverage()).isEqualTo(new BigDecimal("4.3"));
        }

        @Test
        void aSummaryWithNoRatingsAveragesZeroAndShowsNothing() {
            RatingSummary summary = RatingSummary.forProvider("P1");

            assertThat(summary.overallAverage()).isEqualTo(new BigDecimal("0.0"));
            assertThat(summary.categoryAverage(PUNCTUALITY)).isEqualTo(new BigDecimal("0.0"));
            assertThat(summary.categoryCount(PUNCTUALITY)).isZero();
            assertThat(summary.isOverallShown(LAUNCH_LIMITS)).isFalse();
            assertThat(summary.isCategoryShown(PUNCTUALITY, LAUNCH_LIMITS)).isFalse();
            assertThat(summary.ratingCount()).isZero();
        }

        @Test
        void withdrawingAHiddenRatingRemovesItFromTheAverages() {
            RatingSummary summary = aProviderRated(5, 4, 1);

            summary.withdraw(Stars.of(1), Map.of());

            assertThat(summary.overallAverage()).isEqualTo(new BigDecimal("4.5"));
            assertThat(summary.ratingCount()).isEqualTo(2);
        }

        @Test
        void aSummaryCannotWithdrawMoreRatingsThanItHas() {
            RatingSummary helper = RatingSummary.forHelper("aina");

            assertThatThrownBy(() -> helper.withdraw(Stars.of(5), Map.of()))
                    .isInstanceOf(NoRatingToWithdrawException.class)
                    .hasMessageContaining("helper aina");
        }
    }

    @Nested
    class CategoryAverages {

        private RatingSummary punctualityRated5And3AndTwoOthersWithout() {
            RatingSummary summary = RatingSummary.forProvider("P1");
            summary.record(Stars.of(4), Map.of(PUNCTUALITY, Stars.of(5)));
            summary.record(Stars.of(4), Map.of(PUNCTUALITY, Stars.of(3), COMMUNICATION, Stars.of(2)));
            summary.record(Stars.of(4), Map.of("QUALITY_OF_WORK", Stars.of(4)));
            summary.record(Stars.of(4), Map.of("QUALITY_OF_WORK", Stars.of(5)));
            return summary;
        }

        @Test
        void categoryAveragesCountOnlyTheRatingsThatRatedThatCategory() {
            RatingSummary summary = punctualityRated5And3AndTwoOthersWithout();

            assertThat(summary.categoryAverage(PUNCTUALITY)).isEqualTo(new BigDecimal("4.0"));
            assertThat(summary.categoryCount(PUNCTUALITY)).isEqualTo(2);
            assertThat(summary.categoryAverage("QUALITY_OF_WORK")).isEqualTo(new BigDecimal("4.5"));
            assertThat(summary.ratingCount()).isEqualTo(4);
        }

        @Test
        void aCategoryAverageIsShownOnceThatCategoryHasTheMinimumRatings() {
            RatingSummary summary = RatingSummary.forProvider("P1");
            summary.record(Stars.of(4), Map.of(COMMUNICATION, Stars.of(4)));
            summary.record(Stars.of(4), Map.of(COMMUNICATION, Stars.of(5)));
            summary.record(Stars.of(4), Map.of(PUNCTUALITY, Stars.of(4)));
            summary.record(Stars.of(4), Map.of(PUNCTUALITY, Stars.of(4)));
            summary.record(Stars.of(4), Map.of(PUNCTUALITY, Stars.of(4)));

            assertThat(summary.isOverallShown(LAUNCH_LIMITS)).isTrue();
            assertThat(summary.isCategoryShown(COMMUNICATION, LAUNCH_LIMITS)).isFalse();
            assertThat(summary.isCategoryShown(PUNCTUALITY, LAUNCH_LIMITS)).isTrue();
        }

        @Test
        void withdrawingARatingTakesBackItsCategoryStars() {
            RatingSummary summary = punctualityRated5And3AndTwoOthersWithout();

            summary.withdraw(Stars.of(4), Map.of(PUNCTUALITY, Stars.of(3), COMMUNICATION, Stars.of(2)));

            assertThat(summary.categoryAverage(PUNCTUALITY)).isEqualTo(new BigDecimal("5.0"));
            assertThat(summary.categoryCount(PUNCTUALITY)).isEqualTo(1);
            assertThat(summary.categoryCount(COMMUNICATION)).isZero();
            assertThat(summary.categoryAverage(COMMUNICATION)).isEqualTo(new BigDecimal("0.0"));
            assertThat(summary.ratingCount()).isEqualTo(3);
        }

        @Test
        void aCategoryWithdrawnToNothingCannotBeWithdrawnAgain() {
            RatingSummary summary = punctualityRated5And3AndTwoOthersWithout();
            summary.withdraw(Stars.of(4), Map.of(PUNCTUALITY, Stars.of(3), COMMUNICATION, Stars.of(2)));

            assertThatThrownBy(() -> summary.withdraw(Stars.of(4), Map.of(COMMUNICATION, Stars.of(2))))
                    .isInstanceOf(NoRatingToWithdrawException.class);
            assertThat(summary.categoryCount(COMMUNICATION)).isZero();
            assertThat(summary.ratingCount()).isEqualTo(3);
        }

        @Test
        void aCategoryThatWasNeverRatedCannotBeWithdrawn() {
            RatingSummary summary = aProviderRated(4);

            assertThatThrownBy(() -> summary.withdraw(Stars.of(4), Map.of(PUNCTUALITY, Stars.of(3))))
                    .isInstanceOf(NoRatingToWithdrawException.class);
            assertThat(summary.ratingCount()).isEqualTo(1);
        }
    }

    @Nested
    class WhichCategories {

        @Test
        void aProviderIsRatedOnlyOnProviderCategories() {
            RatingSummary provider = RatingSummary.forProvider("P1");

            assertThatThrownBy(() -> provider.record(Stars.of(5), Map.of("FRIENDLINESS", Stars.of(5))))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class)
                    .hasMessage("FRIENDLINESS is not a provider category");
            assertThat(provider.ratingCount()).isZero();
            assertThatThrownBy(() -> provider.categoryAverage("FRIENDLINESS")).isInstanceOf(RatingDoesNotFitSummaryException.class);
        }

        @Test
        void aHelperIsRatedOnlyOnHelperCategories() {
            RatingSummary helper = RatingSummary.forHelper("aina");
            helper.record(Stars.of(5), Map.of("FRIENDLINESS", Stars.of(5)));

            assertThat(helper.categoryAverage("FRIENDLINESS")).isEqualTo(new BigDecimal("5.0"));
            assertThatThrownBy(() -> helper.record(Stars.of(5), Map.of(PUNCTUALITY, Stars.of(5))))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class)
                    .hasMessage("PUNCTUALITY is not a helper category");
            assertThatThrownBy(() -> helper.withdraw(Stars.of(5), Map.of(PUNCTUALITY, Stars.of(5))))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class);
            assertThat(helper.ratingCount()).isEqualTo(1);
        }

        @Test
        void eachSubjectListsItsCategories() {
            assertThat(RatingSummary.Subject.PROVIDER.categories())
                    .containsExactly("QUALITY_OF_WORK", "PUNCTUALITY", "COMMUNICATION", "CARE_FOR_THE_HOME");
            assertThat(RatingSummary.Subject.HELPER.categories())
                    .containsExactly("FRIENDLINESS", "HARD_WORK", "CARE_WITH_BELONGINGS", "COOPERATION", "THOROUGHNESS");
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
