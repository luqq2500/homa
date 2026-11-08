package conversation.domain;

import java.util.UUID;

/** Type-safe generator for Message ids. */
public final class MessageId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
