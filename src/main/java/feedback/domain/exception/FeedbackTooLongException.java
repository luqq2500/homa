package feedback.domain.exception;

/** Written feedback has a length limit. */
public class FeedbackTooLongException extends FeedbackRuleViolationException {

    public FeedbackTooLongException(int length, int limit) {
        super("Written feedback can be at most " + limit + " characters, was " + length);
    }
}
