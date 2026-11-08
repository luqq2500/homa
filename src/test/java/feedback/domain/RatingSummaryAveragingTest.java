package feedback.domain;

import static feedback.domain.FeedbackBuilders.LAUNCH_LIMITS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feedback.domain.exception.FeedbackDetailRequiredException;
import feedback.domain.exception.NoRatingToWithdrawException;
import feedback.domain.exception.RatingDoesNotFitSummaryException;
import java.math.BigDecimal;
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
            summary.record(Stars.of(given), Set.of());
        }
        return summary;
    }

    private static void pick(RatingSummary summary, int times, String pick) {
        for (int i = 0; i < times; i++) {
            summary.record(Stars.of(5), Set.of(pick));
        }
    }

    @Nested
    class AverageStars {

        @ParameterizedTest(name = "with {0} ratings the average is shown: {1}")
        @CsvSource({"2, false", "3, true", "4, true"})
        void averageStarsAreShownOnlyAfterTheMinimumNumberOfRatings(int count, boolean shown) {
            RatingSummary summary = RatingSummary.forProvider("P1");
            for (int i = 0; i < count; i++) {
                summary.record(Stars.of(4), Set.of());
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
            assertThat(summary.topPicks(4)).isEmpty();
        }

        @Test
        void withdrawingAHiddenRatingRemovesItFromTheAverage() {
            RatingSummary summary = aProviderRated(5, 4, 1);

            summary.withdraw(Stars.of(1), Set.of());

            assertThat(summary.averageStars()).isEqualTo(new BigDecimal("4.5"));
            assertThat(summary.ratingCount()).isEqualTo(2);
        }

        @Test
        void aSummaryCannotWithdrawMoreRatingsThanItHas() {
            RatingSummary helper = RatingSummary.forHelper("aina");

            assertThatThrownBy(() -> helper.withdraw(Stars.of(5), Set.of()))
                    .isInstanceOf(NoRatingToWithdrawException.class)
                    .hasMessageContaining("helper aina");
        }
    }

    @Nested
    class Highlights {

        @Test
        void highlightsAreCountedNotAveraged() {
            RatingSummary provider = RatingSummary.forProvider("P1");
            provider.record(Stars.of(5), Set.of("ON_TIME"));
            provider.record(Stars.of(4), Set.of("ON_TIME", "GREAT_WORK"));
            provider.record(Stars.of(3), Set.of("ON_TIME"));
            provider.record(Stars.of(4), Set.of());

            assertThat(provider.topPicks(4)).containsExactly(new PickCount("ON_TIME", 3), new PickCount("GREAT_WORK", 1));
            assertThat(provider.ratingCount()).isEqualTo(4);
        }

        @Test
        void withdrawingAHiddenRatingRemovesItsHighlights() {
            RatingSummary provider = RatingSummary.forProvider("P1");
            pick(provider, 3, "ON_TIME");

            provider.withdraw(Stars.of(5), Set.of("ON_TIME"));

            assertThat(provider.topPicks(4)).containsExactly(new PickCount("ON_TIME", 2));
        }

        @Test
        void aHighlightWithdrawnToNothingIsNoLongerShown() {
            RatingSummary provider = RatingSummary.forProvider("P1");
            provider.record(Stars.of(5), Set.of("ON_TIME", "RESPONSIVE"));
            provider.record(Stars.of(4), Set.of("ON_TIME"));

            provider.withdraw(Stars.of(5), Set.of("ON_TIME", "RESPONSIVE"));

            assertThat(provider.topPicks(4)).containsExactly(new PickCount("ON_TIME", 1));
            assertThat(provider.averageStars()).isEqualTo(new BigDecimal("4.0"));
        }

        @Test
        void aHighlightThatWasNeverPickedCannotBeWithdrawn() {
            RatingSummary provider = RatingSummary.forProvider("P1");
            provider.record(Stars.of(4), Set.of("ON_TIME"));

            assertThatThrownBy(() -> provider.withdraw(Stars.of(4), Set.of("RESPONSIVE")))
                    .isInstanceOf(NoRatingToWithdrawException.class);
            assertThat(provider.ratingCount()).isEqualTo(1);
        }

        @Test
        void aProviderIsPickedOnlyForHighlights() {
            RatingSummary provider = RatingSummary.forProvider("P1");

            assertThatThrownBy(() -> provider.record(Stars.of(5), Set.of("FRIENDLY")))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class)
                    .hasMessage("FRIENDLY is not a highlight");
            assertThat(provider.ratingCount()).isZero();
        }
    }

    @Nested
    class Compliments {

        @Test
        void theMostGivenComplimentsComeFirst() {
            RatingSummary helper = RatingSummary.forHelper("aina");
            pick(helper, 1, "COOPERATIVE");
            pick(helper, 3, "FRIENDLY");
            pick(helper, 2, "THOROUGH");

            assertThat(helper.topPicks(2)).containsExactly(new PickCount("FRIENDLY", 3), new PickCount("THOROUGH", 2));
            assertThat(helper.topPicks(5)).containsExactly(
                    new PickCount("FRIENDLY", 3), new PickCount("THOROUGH", 2), new PickCount("COOPERATIVE", 1));
        }

        @Test
        void complimentsPickedEquallyOftenKeepTheirListedOrder() {
            RatingSummary helper = RatingSummary.forHelper("aina");
            pick(helper, 2, "THOROUGH");
            pick(helper, 2, "COOPERATIVE");
            pick(helper, 2, "FRIENDLY");

            assertThat(helper.topPicks(3)).extracting(PickCount::pick).containsExactly("FRIENDLY", "COOPERATIVE", "THOROUGH");
        }

        @Test
        void aHelperIsPickedOnlyForCompliments() {
            assertThatThrownBy(() -> RatingSummary.forHelper("aina").record(Stars.of(5), Set.of("ON_TIME")))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class)
                    .hasMessage("ON_TIME is not a compliment");
        }

        @Test
        void aPickOfTheWrongKindCannotBeWithdrawnEither() {
            RatingSummary helper = RatingSummary.forHelper("aina");
            helper.record(Stars.of(5), Set.of("FRIENDLY"));

            assertThatThrownBy(() -> helper.withdraw(Stars.of(5), Set.of("ON_TIME")))
                    .isInstanceOf(RatingDoesNotFitSummaryException.class);
            assertThat(helper.ratingCount()).isEqualTo(1);
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
