package conversation.domain.exception;

/** Only the customer can start a chat; the provider can reply. */
public class OnlyCustomerCanStartChatException extends ConversationRuleViolationException {

    public OnlyCustomerCanStartChatException() {
        super("Only the customer can start a chat");
    }
}
