package feedback.domain;

import static feedback.domain.FeedbackBuilders.LAUNCH_LIMITS;
import static feedback.domain.FeedbackBuilders.aCompletedReservation;
import static feedback.domain.FeedbackBuilders.daysAfterSaturday;
import static feedback.domain.FeedbackBuilders.stars;
import static feedback.domain.FeedbackBuilders.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feedback.domain.exception.AlreadyRatedException;
import feedback.domain.exception.FeedbackTooLongException;
import feedback.domain.exception.HelperNotOnReservationException;
import feedback.domain.exception.NotTheReservationCustomerException;
import feedback.domain.exception.OverallStarsRequiredException;
import feedback.domain.exception.RatingWindowClosedException;
import feedback.domain.exception.StarsOutOfRangeException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Feature: Rating a completed reservation. Background: a reservation that became Completed on Saturday
 * 14:00; the customer is C1, the provider is P1, and the named helpers are Aina and Ben.
 */
class RatingSubmissionTest {

    private static Rating rate(ProviderRating provider, Map<String, HelperRating> helpers, Instant at) {
        return Rating.submit(aCompletedReservation(), "C1", provider, helpers, LAUNCH_LIMITS, at);
    }

    @Nested
    class TheProviderRating {

        @Test
        void theCustomerRatesTheProvider() {
            Rating rating = rate(stars(4), Map.of(), sunday("10:00"));

            assertThat(rating.provider().overall()).isEqualTo(Stars.of(4));
            assertThat(rating.reservationId()).isEqualTo("R1");
            assertThat(rating.customerId()).isEqualTo("C1");
            assertThat(rating.providerId()).isEqualTo("P1");
            assertThat(rating.submittedAt()).isEqualTo(sunday("10:00"));
            assertThat(rating.id()).isNotBlank();
            assertThat(rating.isHidden()).isFalse();
        }

        @ParameterizedTest(name = "{0} stars are recorded")
        @ValueSource(ints = {1, 5})
        void starsAreWholeNumbersFromOneToFive(int given) {
            assertThat(rate(stars(given), Map.of(), sunday("10:00")).provider().overall().value()).isEqualTo(given);
        }

        @ParameterizedTest(name = "{0} stars are rejected")
        @ValueSource(ints = {0, 6})
        void starsOutsideOneToFiveAreRejected(int given) {
            assertThatThrownBy(() -> rate(stars(given), Map.of(), sunday("10:00")))
                    .isInstanceOf(StarsOutOfRangeException.class)
                    .hasMessageContaining(String.valueOf(given));
        }

        @Test
        void writtenFeedbackIsOptional() {
            ProviderRating given = ProviderRating.of(Stars.of(5), Map.of(ProviderCategory.PUNCTUALITY, Stars.of(5)), null);

            Rating rating = rate(given, Map.of(), sunday("10:00"));

            assertThat(rating.provider().categories()).containsExactly(Map.entry(ProviderCategory.PUNCTUALITY, Stars.of(5)));
            assertThat(rating.provider().hasWrittenFeedback()).isFalse();
            assertThat(rating.provider().writtenFeedback()).isEmpty();
        }

        @Test
        void providerCategoryStarsAreOptional() {
            Rating rating = rate(ProviderRating.of(Stars.of(4)), Map.of(), sunday("10:00"));

            assertThat(rating.provider().overall()).isEqualTo(Stars.of(4));
            assertThat(rating.provider().categories()).isEmpty();
            assertThat(rating.provider().hasWrittenFeedback()).isFalse();
        }

        @Test
        void overallStarsAndTextWithoutCategoryStarsAreRecorded() {
            Rating rating = rate(ProviderRating.of(Stars.of(3), Map.of(), " Arrived late "), Map.of(), sunday("10:00"));

            assertThat(rating.provider().categories()).isEmpty();
            assertThat(rating.provider().writtenFeedback()).isEqualTo("Arrived late");
        }

        @Test
        void theProviderRatingNeedsOverallStars() {
            assertThatThrownBy(() -> ProviderRating.of(null, Map.of(ProviderCategory.QUALITY_OF_WORK, Stars.of(4)), ""))
                    .isInstanceOf(OverallStarsRequiredException.class);
        }

        @Test
        void anyNumberOfProviderCategoriesCanBeRated() {
            ProviderRating given = ProviderRating.of(Stars.of(4), Map.of(
                    ProviderCategory.QUALITY_OF_WORK, Stars.of(5),
                    ProviderCategory.PUNCTUALITY, Stars.of(3),
                    ProviderCategory.COMMUNICATION, Stars.of(4),
                    ProviderCategory.CARE_FOR_THE_HOME, Stars.of(5)), " Spotless kitchen ");

            Rating rating = rate(given, Map.of(), sunday("10:00"));

            assertThat(rating.provider().overall()).isEqualTo(Stars.of(4));
            assertThat(rating.provider().categories()).hasSize(4).containsEntry(ProviderCategory.PUNCTUALITY, Stars.of(3));
            assertThat(rating.provider().writtenFeedback()).isEqualTo("Spotless kitchen");
            assertThat(rating.provider().hasWrittenFeedback()).isTrue();
        }

        @Test
        void writtenFeedbackHasALengthLimit() {
            Map<ProviderCategory, Stars> punctuality = Map.of(ProviderCategory.PUNCTUALITY, Stars.of(4));

            assertThatThrownBy(() -> ProviderRating.of(Stars.of(4), punctuality, "a".repeat(1_001)))
                    .isInstanceOf(FeedbackTooLongException.class)
                    .hasMessageContaining("1001");
            assertThat(ProviderRating.of(Stars.of(4), punctuality, "a".repeat(1_000)).writtenFeedback()).hasSize(1_000);
        }

        @Test
        void theLengthLimitCountsCharactersNotBytes() {
            String emoji = "😊";
            Map<ProviderCategory, Stars> punctuality = Map.of(ProviderCategory.PUNCTUALITY, Stars.of(4));

            assertThat(ProviderRating.of(Stars.of(4), punctuality, emoji.repeat(1_000)).hasWrittenFeedback()).isTrue();
            assertThatThrownBy(() -> ProviderRating.of(Stars.of(4), punctuality, emoji.repeat(1_001)))
                    .isInstanceOf(FeedbackTooLongException.class);
        }
    }

    @Nested
    class WhoAndWhen {

        @Test
        void onlyTheReservationsCustomerCanRateIt() {
            assertThatThrownBy(() -> Rating.submit(aCompletedReservation(), "C2", stars(4), Map.of(), LAUNCH_LIMITS, sunday("10:00")))
                    .isInstanceOf(NotTheReservationCustomerException.class)
                    .hasMessageContaining("C2");
        }

        @Test
        void aReservationIsRatedOnce() {
            RateableReservation alreadyRated = aCompletedReservation().rated();

            assertThatThrownBy(() -> Rating.submit(alreadyRated, "C1", stars(4), Map.of(), LAUNCH_LIMITS, sunday("10:00")))
                    .isInstanceOf(AlreadyRatedException.class)
                    .hasMessageContaining("R1");
        }

        static Stream<Arguments> windowEdges() {
            return Stream.of(
                    Arguments.of(daysAfterSaturday(14, "14:00"), true),
                    Arguments.of(daysAfterSaturday(14, "14:01"), false),
                    Arguments.of(daysAfterSaturday(14, "14:00").plusSeconds(1), false));
        }

        @ParameterizedTest(name = "rating at {0} is accepted: {1}")
        @MethodSource("windowEdges")
        void ratingsAreAcceptedOnlyWithinTheRatingWindow(Instant at, boolean accepted) {
            if (accepted) {
                assertThat(rate(stars(4), Map.of(), at).submittedAt()).isEqualTo(at);
            } else {
                assertThatThrownBy(() -> rate(stars(4), Map.of(), at)).isInstanceOf(RatingWindowClosedException.class);
            }
        }
    }

    @Nested
    class Helpers {

        @Test
        void onlyHelpersWhoWereNamedCanBeRated() {
            assertThatThrownBy(() -> rate(stars(4), Map.of("chan", HelperRating.of(Stars.of(5))), sunday("10:00")))
                    .isInstanceOf(HelperNotOnReservationException.class)
                    .hasMessageContaining("chan");
        }

        @Test
        void helperRatingsAreOptional() {
            assertThat(rate(stars(4), Map.of(), sunday("10:00")).helpers()).isEmpty();
        }

        @Test
        void aHelperCanReceiveOverallAndCategoryStars() {
            HelperRating given = HelperRating.of(Stars.of(5), Map.of(
                    HelperCategory.FRIENDLINESS, Stars.of(5), HelperCategory.THOROUGHNESS, Stars.of(4)));

            HelperRating aina = rate(stars(4), Map.of("aina", given), sunday("10:00")).helpers().get("aina");

            assertThat(aina.overall()).isEqualTo(Stars.of(5));
            assertThat(aina.categories()).hasSize(2).containsEntry(HelperCategory.THOROUGHNESS, Stars.of(4));
        }

        @Test
        void aRatedHelperNeedsOverallStars() {
            assertThatThrownBy(() -> HelperRating.of(null, Map.of(HelperCategory.FRIENDLINESS, Stars.of(5))))
                    .isInstanceOf(OverallStarsRequiredException.class);
        }

        @Test
        void helperCategoryStarsAreOptional() {
            HelperRating aina = rate(stars(4), Map.of("aina", HelperRating.of(Stars.of(4))), sunday("10:00")).helpers().get("aina");

            assertThat(aina.overall()).isEqualTo(Stars.of(4));
            assertThat(aina.categories()).isEmpty();
        }

        @Test
        void bothNamedHelpersCanBeRated() {
            Rating rating = rate(stars(4), Map.of("aina", HelperRating.of(Stars.of(5)), "ben", HelperRating.of(Stars.of(3))), sunday("10:00"));

            assertThat(rating.helpers()).containsOnlyKeys("aina", "ben");
        }
    }
}
