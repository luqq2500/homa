package feedback.domain;

import feedback.domain.exception.CategoryRatingRequiredException;
import feedback.domain.exception.FeedbackTooLongException;
import feedback.domain.exception.OverallStarsRequiredException;
import java.util.Map;

/**
 * What the customer says about the provider: overall stars (required), stars for at least one provider
 * category, and written feedback (optional, up to 1,000 characters; empty when there is none).
 */
public record ProviderRating(Stars overall, Map<ProviderCategory, Stars> categories, String writtenFeedback) {

    public static final int FEEDBACK_LIMIT = 1_000;

    public ProviderRating {
        if (overall == null) {
            throw new OverallStarsRequiredException();
        }
        categories = Map.copyOf(categories);
        if (categories.isEmpty()) {
            throw new CategoryRatingRequiredException();
        }
        writtenFeedback = writtenFeedback == null ? "" : writtenFeedback.strip();
        int length = writtenFeedback.codePointCount(0, writtenFeedback.length());
        if (length > FEEDBACK_LIMIT) {
            throw new FeedbackTooLongException(length, FEEDBACK_LIMIT);
        }
    }

    public static ProviderRating of(Stars overall, Map<ProviderCategory, Stars> categories, String writtenFeedback) {
        return new ProviderRating(overall, categories, writtenFeedback);
    }

    public boolean hasWrittenFeedback() {
        return !writtenFeedback.isEmpty();
    }
}
