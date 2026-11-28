package com.homa.conversation.usecase.dto;

import java.time.Instant;

/**
 * The message that was sent.
 *
 * @param author CUSTOMER or PROVIDER: which party the sender is in this chat
 */
public record SendMessageResponse(String conversationId, String messageId, String author, String text,
        Instant sentAt) {
}
