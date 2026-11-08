package conversation.domain;

import conversation.domain.exception.MessageTextRequiredException;
import java.time.Instant;
import java.util.Objects;

/** One message in a chat. */
public final class Message {

    private final String id;
    private final Party sender;
    private final String text;
    private final Instant sentAt;

    Message(Party sender, String text, Instant sentAt) {
        if (text == null || text.isBlank()) {
            throw new MessageTextRequiredException();
        }
        this.id = new MessageId().generate();
        this.sender = Objects.requireNonNull(sender);
        this.text = text.strip();
        this.sentAt = Objects.requireNonNull(sentAt);
    }

    public String id() {
        return id;
    }

    public Party sender() {
        return sender;
    }

    public String text() {
        return text;
    }

    public Instant sentAt() {
        return sentAt;
    }
}
