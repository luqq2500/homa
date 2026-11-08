package conversation.domain;

import conversation.domain.exception.ConversationDetailRequiredException;
import java.util.Objects;

/** What a chat belongs to: an enquiry, or a reservation. */
public record ChatSubject(Kind kind, String id) {

    public enum Kind {
        ENQUIRY,
        RESERVATION
    }

    public ChatSubject {
        if (id == null || id.isBlank()) {
            throw new ConversationDetailRequiredException("A subject id");
        }
        Objects.requireNonNull(kind);
    }

    public static ChatSubject enquiry(String enquiryId) {
        return new ChatSubject(Kind.ENQUIRY, enquiryId);
    }

    public static ChatSubject reservation(String reservationId) {
        return new ChatSubject(Kind.RESERVATION, reservationId);
    }
}
