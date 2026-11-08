package conversation.domain.exception;

/** An archived chat stays readable but takes no new messages. */
public class ConversationArchivedException extends ConversationRuleViolationException {

    public ConversationArchivedException() {
        super("This chat is archived");
    }
}
