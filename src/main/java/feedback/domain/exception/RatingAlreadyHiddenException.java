package feedback.domain.exception;

/** A rating is hidden once. */
public class RatingAlreadyHiddenException extends FeedbackRuleViolationException {

    public RatingAlreadyHiddenException(String ratingId) {
        super("Rating " + ratingId + " is already hidden");
    }
}
