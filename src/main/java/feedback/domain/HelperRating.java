package feedback.domain;

import feedback.domain.exception.OverallStarsRequiredException;
import java.util.Map;

/** What the customer says about one helper: overall stars (required) and stars for any helper category. No text. */
public record HelperRating(Stars overall, Map<HelperCategory, Stars> categories) {

    public HelperRating {
        if (overall == null) {
            throw new OverallStarsRequiredException();
        }
        categories = Map.copyOf(categories);
    }

    public static HelperRating of(Stars overall) {
        return new HelperRating(overall, Map.of());
    }

    public static HelperRating of(Stars overall, Map<HelperCategory, Stars> categories) {
        return new HelperRating(overall, categories);
    }
}
