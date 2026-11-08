package conversation.domain.exception;

/** Ids cannot be left empty. */
public class ConversationDetailRequiredException extends ConversationRuleViolationException {

    public ConversationDetailRequiredException(String what) {
        super(what + " is required");
    }
}
