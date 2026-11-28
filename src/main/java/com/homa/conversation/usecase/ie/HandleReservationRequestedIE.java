package com.homa.conversation.usecase.ie;

import com.homa.conversation.domain.Conversation;
import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.ProviderId;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.ReservationId;
import com.homa.conversation.usecase.port.in.ReservationRequestedIEHandler;
import com.homa.conversation.usecase.port.out.ConversationRepository;
import com.homa.conversation.usecase.port.out.ProcessedMessageStore;
import com.homa.conversation.usecase.port.out.ReservationChatWindowRepository;
import com.homa.ie.events.ReservationRequestedIE;

/**
 * CHT-1, CHT-2: the customer's reservation is Requested, so its chat opens between them and the provider, and stays
 * open until the Protection Window ends. The window is kept first, from the event's {@code protectionWindowEnd}; if
 * the reservation's withdrawal, decline or expiry was somehow heard first, the window keeps that end and the chat
 * opens read-only. A reservation has one chat, so a second request for it opens no second one. Publishes nothing.
 */
public final class HandleReservationRequestedIE implements ReservationRequestedIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "conversation.ReservationRequested";

    private final ProcessedMessageStore processedMessages;
    private final ReservationChatWindowRepository windows;
    private final ConversationRepository conversations;

    public HandleReservationRequestedIE(ProcessedMessageStore processedMessages,
            ReservationChatWindowRepository windows, ConversationRepository conversations) {
        this.processedMessages = processedMessages;
        this.windows = windows;
        this.conversations = conversations;
    }

    @Override
    public void handle(ReservationRequestedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ReservationId reservationId = new ReservationId(event.reservationId());
        ReservationChatWindow window = windows.findByReservationId(reservationId)
                .map(heard -> heard.withProtectionWindowEnd(event.protectionWindowEnd()))
                .orElseGet(() -> ReservationChatWindow.open(reservationId, new CustomerId(event.customerId()),
                        new ProviderId(event.providerId()), event.protectionWindowEnd()));
        windows.save(window);
        if (conversations.findByReservationId(reservationId).isEmpty()) {
            conversations.save(Conversation.openReservationChat(window, event.requestedAt()));
        }
    }
}
