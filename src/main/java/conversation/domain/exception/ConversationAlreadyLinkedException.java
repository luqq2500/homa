package conversation.domain.exception;

/** An enquiry's chat carries on as one reservation's chat. */
public class ConversationAlreadyLinkedException extends ConversationRuleViolationException {

    public ConversationAlreadyLinkedException(String reservationId) {
        super("This chat already belongs to reservation " + reservationId);
    }
}
