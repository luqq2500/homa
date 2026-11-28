package com.homa.conversation.domain;

import java.util.Optional;

/**
 * What Conversation knows of a reservation, for its chat (CHT-5): either the chat runs until the Protection Window
 * ends, or the reservation ended before its Check-in Period and the chat ended with it.
 */
public enum ChatWindowStatus {
    /**
     * Requested, Reserved, Completed, or cancelled once the Check-in Period had started: the chat is open until the
     * Protection Window ends (CHT-2, CHT-5).
     */
    OPEN(null),
    WITHDRAWN(ChatReadOnlyReason.RESERVATION_WITHDRAWN),
    DECLINED(ChatReadOnlyReason.RESERVATION_DECLINED),
    EXPIRED(ChatReadOnlyReason.RESERVATION_EXPIRED),
    CANCELLED_BEFORE_CHECK_IN(ChatReadOnlyReason.RESERVATION_CANCELLED_BEFORE_CHECK_IN);

    private final ChatReadOnlyReason readOnlyBecause;

    ChatWindowStatus(ChatReadOnlyReason readOnlyBecause) {
        this.readOnlyBecause = readOnlyBecause;
    }

    /** Why the chat is read-only at once under this status; empty while the chat runs until the window ends. */
    public Optional<ChatReadOnlyReason> readOnlyReason() {
        return Optional.ofNullable(readOnlyBecause);
    }
}
