package feedback.domain.exception;

/** Highlights belong to providers and compliments to helpers. */
public class RatingDoesNotFitSummaryException extends FeedbackRuleViolationException {

    public RatingDoesNotFitSummaryException(String detail) {
        super(detail);
    }
}
