package com.homa.conversation.usecase.dto;

import java.time.Instant;
import java.util.List;

/**
 * A chat as one of its parties reads it. The apps show Homa's off-platform reminder (CHT-3) on every chat; it is
 * not part of this response.
 *
 * @param kind           ENQUIRY or RESERVATION
 * @param reservationId  the reservation this is the chat of; null for an Enquiry
 * @param yourRole       CUSTOMER or PROVIDER: which party the viewer is in this chat
 * @param writable       whether the viewer can write a message now (CHT-2, CHT-5)
 * @param readOnlyReason why the chat can only be read: PROTECTION_WINDOW_ENDED, RESERVATION_WITHDRAWN,
 *                       RESERVATION_DECLINED, RESERVATION_EXPIRED or RESERVATION_CANCELLED_BEFORE_CHECK_IN; null
 *                       while it is writable
 * @param writableUntil  when a reservation's open chat turns read-only, its Protection Window end; null for an
 *                       Enquiry and for a chat that can only be read
 * @param messages       every message, in the order they were sent
 */
public record ViewConversationResponse(
        String conversationId,
        String kind,
        String customerId,
        String providerId,
        String reservationId,
        String yourRole,
        boolean writable,
        String readOnlyReason,
        Instant writableUntil,
        List<MessageView> messages) {

    public ViewConversationResponse {
        messages = List.copyOf(messages);
    }

    /** One message of the chat. {@code author} is CUSTOMER or PROVIDER. */
    public record MessageView(String messageId, String author, String text, Instant sentAt) {
    }
}
