package com.homa.conversation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Whether a chat can be written to at a given moment, and if it can't, why (CHT-2, CHT-5). It is computed from the
 * reservation's {@link ReservationChatWindow} and the clock each time it is asked; nothing stores it, so nothing has
 * to flip it when the Protection Window ends.
 */
public final class ChatAccess {

    private static final ChatAccess OPEN_WITHOUT_END = new ChatAccess(null, null);

    private final ChatReadOnlyReason readOnlyReason;
    private final Instant openUntil;

    private ChatAccess(ChatReadOnlyReason readOnlyReason, Instant openUntil) {
        this.readOnlyReason = readOnlyReason;
        this.openUntil = openUntil;
    }

    /** An Enquiry stays open: no rule ends it. */
    static ChatAccess open() {
        return OPEN_WITHOUT_END;
    }

    /** A reservation's chat is open until its Protection Window ends. */
    static ChatAccess openUntil(Instant protectionWindowEnd) {
        return new ChatAccess(null, Objects.requireNonNull(protectionWindowEnd, "protectionWindowEnd"));
    }

    static ChatAccess readOnly(ChatReadOnlyReason reason) {
        return new ChatAccess(Objects.requireNonNull(reason, "reason"), null);
    }

    public boolean isOpen() {
        return readOnlyReason == null;
    }

    /** Why the chat can only be read; empty while it is open. */
    public Optional<ChatReadOnlyReason> readOnlyReason() {
        return Optional.ofNullable(readOnlyReason);
    }

    /** The moment a reservation's open chat turns read-only, its Protection Window end. Empty for an Enquiry and a read-only chat. */
    public Optional<Instant> openUntil() {
        return Optional.ofNullable(openUntil);
    }
}
