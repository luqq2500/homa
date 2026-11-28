package com.homa.conversation.domain;

import com.homa.conversation.domain.exception.EmptyMessageException;
import java.time.Instant;
import java.util.Objects;

/** One thing a party said, as they said it, and when. Homa doesn't edit, filter or translate it (CHT-3). */
public record Message(MessageId id, PartyRole author, String text, Instant sentAt) {

    public Message {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(sentAt, "sentAt");
        if (text == null || text.isBlank()) {
            throw new EmptyMessageException();
        }
    }

    /** A new message from {@code author}, sent at {@code sentAt}. */
    static Message sentBy(PartyRole author, String text, Instant sentAt) {
        return new Message(MessageId.generate(), author, text, sentAt);
    }
}
