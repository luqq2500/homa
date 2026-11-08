package feedback.domain.exception;

/** Category ratings belong to providers and compliments to helpers. */
public class RatingDoesNotFitSummaryException extends FeedbackRuleViolationException {

    public RatingDoesNotFitSummaryException(String detail) {
        super(detail);
    }
}
