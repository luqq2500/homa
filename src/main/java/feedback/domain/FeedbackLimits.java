package feedback.domain;

import java.time.Duration;
import java.util.Objects;

/** The Homa-set values feedback uses: the rating window and the minimum ratings to show an average. */
public record FeedbackLimits(Duration ratingWindow, int minimumRatingsToShow) {

    public FeedbackLimits {
        Objects.requireNonNull(ratingWindow);
    }
}
