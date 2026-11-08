package feedback.domain.exception;

/** A rule of ratings was broken. Every feedback domain exception is one of these. */
public abstract class FeedbackRuleViolationException extends RuntimeException {

    protected FeedbackRuleViolationException(String message) {
        super(message);
    }
}
