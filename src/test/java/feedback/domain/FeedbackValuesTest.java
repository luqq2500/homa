package feedback.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import feedback.domain.exception.StarsOutOfRangeException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Stars, provider and helper ratings, and the Homa-set values feedback uses. */
class FeedbackValuesTest {

    @Test
    void starsAreFromOneToFive() {
        assertThat(Stars.of(1).value()).isEqualTo(1);
        assertThat(Stars.of(5).value()).isEqualTo(5);
        assertThatThrownBy(() -> Stars.of(0)).isInstanceOf(StarsOutOfRangeException.class);
        assertThatThrownBy(() -> Stars.of(6)).isInstanceOf(StarsOutOfRangeException.class);
    }

    @Test
    void aRatingKeepsWhatWasGivenEvenIfTheOriginalChanges() {
        Set<Highlight> highlights = new HashSet<>(Set.of(Highlight.ON_TIME));
        Set<Compliment> compliments = new HashSet<>(Set.of(Compliment.FRIENDLY));
        ProviderRating provider = ProviderRating.of(Stars.of(4), highlights, null);
        HelperRating helper = new HelperRating(Stars.of(5), compliments);

        highlights.clear();
        compliments.clear();

        assertThat(provider.highlights()).containsExactly(Highlight.ON_TIME);
        assertThat(provider.writtenFeedback()).isEmpty();
        assertThat(helper.compliments()).containsExactly(Compliment.FRIENDLY);
    }

    @Test
    void aProviderRatingNeedsOverallStars() {
        assertThatThrownBy(() -> ProviderRating.of(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new HelperRating(null, Set.of())).isInstanceOf(NullPointerException.class);
    }

    @Test
    void aPickCountNamesItsPick() {
        assertThat(new PickCount("ON_TIME", 8).count()).isEqualTo(8);
        assertThatThrownBy(() -> new PickCount(null, 1)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void providersArePickedForHighlightsAndHelpersForComplimentsInTheirListedOrder() {
        assertThat(RatingSummary.Subject.PROVIDER.picks()).containsExactly("GREAT_WORK", "ON_TIME", "RESPONSIVE", "CAREFUL_WITH_THE_HOME");
        assertThat(RatingSummary.Subject.HELPER.picks())
                .containsExactly("FRIENDLY", "HARDWORKING", "CAREFUL_WITH_BELONGINGS", "COOPERATIVE", "THOROUGH");
    }

    @Test
    void feedbackLimitsNeedARatingWindow() {
        assertThat(new FeedbackLimits(Duration.ofDays(14), 3).minimumRatingsToShow()).isEqualTo(3);
        assertThatThrownBy(() -> new FeedbackLimits(null, 3)).isInstanceOf(NullPointerException.class);
    }
}
