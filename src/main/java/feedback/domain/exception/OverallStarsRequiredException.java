package feedback.domain.exception;

/** Every provider or helper rating has overall stars. */
public class OverallStarsRequiredException extends FeedbackRuleViolationException {

    public OverallStarsRequiredException() {
        super("A rating needs overall stars");
    }
}
