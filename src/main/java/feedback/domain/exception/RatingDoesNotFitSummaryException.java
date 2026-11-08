package feedback.domain.exception;

/** Provider categories belong to provider summaries, and helper categories to helper summaries. */
public class RatingDoesNotFitSummaryException extends FeedbackRuleViolationException {

    public RatingDoesNotFitSummaryException(String detail) {
        super(detail);
    }
}
