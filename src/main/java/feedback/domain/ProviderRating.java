package feedback.domain;

import feedback.domain.exception.FeedbackTooLongException;
import java.util.Objects;
import java.util.Set;

/**
 * What the customer says about the provider: stars (required), any number of highlights (optional), and
 * written feedback (optional, up to 1,000 characters; empty when there is none).
 */
public record ProviderRating(Stars stars, Set<Highlight> highlights, String writtenFeedback) {

    public static final int FEEDBACK_LIMIT = 1_000;

    public ProviderRating {
        Objects.requireNonNull(stars);
        highlights = Set.copyOf(highlights);
        writtenFeedback = writtenFeedback == null ? "" : writtenFeedback.strip();
        int length = writtenFeedback.codePointCount(0, writtenFeedback.length());
        if (length > FEEDBACK_LIMIT) {
            throw new FeedbackTooLongException(length, FEEDBACK_LIMIT);
        }
    }

    /** Stars only, with no highlights and no written feedback. */
    public static ProviderRating of(Stars stars) {
        return new ProviderRating(stars, Set.of(), "");
    }

    public static ProviderRating of(Stars stars, Set<Highlight> highlights, String writtenFeedback) {
        return new ProviderRating(stars, highlights, writtenFeedback);
    }

    public boolean hasWrittenFeedback() {
        return !writtenFeedback.isEmpty();
    }
}
