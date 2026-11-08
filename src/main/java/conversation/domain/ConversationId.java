package conversation.domain;

import java.util.UUID;

/** Type-safe generator for Conversation ids. */
public final class ConversationId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
