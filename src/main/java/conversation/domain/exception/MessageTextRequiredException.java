package conversation.domain.exception;

/** A message has some text. */
public class MessageTextRequiredException extends ConversationRuleViolationException {

    public MessageTextRequiredException() {
        super("A message needs some text");
    }
}
