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
import feedback.domain.exception.RatingWindowClosedException;
import feedback.domain.exception.StarsOutOfRangeException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
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

            assertThat(rating.provider().stars()).isEqualTo(Stars.of(4));
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
            assertThat(rate(stars(given), Map.of(), sunday("10:00")).provider().stars().value()).isEqualTo(given);
        }

        @ParameterizedTest(name = "{0} stars are rejected")
        @ValueSource(ints = {0, 6})
        void starsOutsideOneToFiveAreRejected(int given) {
            assertThatThrownBy(() -> rate(stars(given), Map.of(), sunday("10:00")))
                    .isInstanceOf(StarsOutOfRangeException.class)
                    .hasMessageContaining(String.valueOf(given));
        }

        @Test
        void highlightsAndWrittenFeedbackAreOptional() {
            Rating rating = rate(stars(5), Map.of(), sunday("10:00"));

            assertThat(rating.provider().highlights()).isEmpty();
            assertThat(rating.provider().hasWrittenFeedback()).isFalse();
            assertThat(rating.provider().writtenFeedback()).isEmpty();
        }

        @Test
        void theCustomerPicksAnyNumberOfHighlights() {
            ProviderRating given = ProviderRating.of(Stars.of(4), Set.of(Highlight.ON_TIME, Highlight.RESPONSIVE), "");

            Rating rating = rate(given, Map.of(), sunday("10:00"));

            assertThat(rating.provider().stars()).isEqualTo(Stars.of(4));
            assertThat(rating.provider().highlights()).containsExactlyInAnyOrder(Highlight.ON_TIME, Highlight.RESPONSIVE);
        }

        @Test
        void everyHighlightCanBePickedTogetherWithWrittenFeedback() {
            ProviderRating given = ProviderRating.of(Stars.of(5), Set.of(Highlight.values()), " Spotless kitchen, on time ");

            Rating rating = rate(given, Map.of(), sunday("10:00"));

            assertThat(rating.provider().highlights()).hasSize(4);
            assertThat(rating.provider().writtenFeedback()).isEqualTo("Spotless kitchen, on time");
            assertThat(rating.provider().hasWrittenFeedback()).isTrue();
        }

        @Test
        void writtenFeedbackHasALengthLimit() {
            assertThatThrownBy(() -> ProviderRating.of(Stars.of(4), Set.of(), "a".repeat(1_001)))
                    .isInstanceOf(FeedbackTooLongException.class)
                    .hasMessageContaining("1001");
            assertThat(ProviderRating.of(Stars.of(4), Set.of(), "a".repeat(1_000)).writtenFeedback()).hasSize(1_000);
        }

        @Test
        void theLengthLimitCountsCharactersNotBytes() {
            String emoji = "😊";

            assertThat(ProviderRating.of(Stars.of(4), Set.of(), emoji.repeat(1_000)).hasWrittenFeedback()).isTrue();
            assertThatThrownBy(() -> ProviderRating.of(Stars.of(4), Set.of(), emoji.repeat(1_001)))
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
        void aHelperCanReceiveStarsAndCompliments() {
            Rating rating = rate(
                    stars(4), Map.of("aina", HelperRating.of(Stars.of(5), Compliment.FRIENDLY, Compliment.THOROUGH)), sunday("10:00"));

            HelperRating aina = rating.helpers().get("aina");
            assertThat(aina.stars()).isEqualTo(Stars.of(5));
            assertThat(aina.compliments()).containsExactlyInAnyOrder(Compliment.FRIENDLY, Compliment.THOROUGH);
        }

        @Test
        void bothNamedHelpersCanBeRated() {
            Rating rating = rate(stars(4), Map.of(
                    "aina", HelperRating.of(Stars.of(5)),
                    "ben", new HelperRating(Stars.of(3), Set.of())), sunday("10:00"));

            assertThat(rating.helpers()).containsOnlyKeys("aina", "ben");
            assertThat(rating.helpers().get("ben").compliments()).isEmpty();
        }
    }
}
