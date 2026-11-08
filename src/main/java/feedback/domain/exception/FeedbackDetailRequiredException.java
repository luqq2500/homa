package feedback.domain.exception;

/** Ids cannot be left empty. */
public class FeedbackDetailRequiredException extends FeedbackRuleViolationException {

    public FeedbackDetailRequiredException(String what) {
        super(what + " is required");
    }
}
