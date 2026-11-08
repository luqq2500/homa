package conversation.domain.exception;

/** A reservation's chat stays open for the life of the reservation. */
public class ReservationChatCannotBeArchivedException extends ConversationRuleViolationException {

    public ReservationChatCannotBeArchivedException() {
        super("A reservation's chat stays open");
    }
}
