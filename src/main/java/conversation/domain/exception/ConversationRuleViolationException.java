package conversation.domain.exception;

/** A chat rule was broken. Every conversation domain exception is one of these. */
public abstract class ConversationRuleViolationException extends RuntimeException {

    protected ConversationRuleViolationException(String message) {
        super(message);
    }
}
