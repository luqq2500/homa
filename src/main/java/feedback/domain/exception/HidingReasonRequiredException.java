package feedback.domain.exception;

/** Homa records why it hides a rating. */
public class HidingReasonRequiredException extends FeedbackRuleViolationException {

    public HidingReasonRequiredException() {
        super("Hiding a rating needs a reason");
    }
}
