package com.homa.conversation.usecase.ie;

import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.ProviderId;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.ReservationId;
import com.homa.conversation.usecase.port.in.ReservationCancelledIEHandler;
import com.homa.conversation.usecase.port.out.ProcessedMessageStore;
import com.homa.conversation.usecase.port.out.ReservationChatWindowRepository;
import com.homa.ie.events.ReservationCancelledIE;

/**
 * CHT-5: the reservation was cancelled. Its chat is read-only at once only when {@code cancelledAt} is before
 * {@code checkInStart}; from the moment the Check-in Period starts, the chat stays open until the Protection Window
 * ends. The domain makes that call, whatever the cause or the settlement (Conversation reads neither). Only the
 * reservation's chat window changes. If the cancellation is heard before the request, the window is created from
 * the cancellation, which carries the Protection Window end, and the request later finds it. Publishes nothing.
 */
public final class HandleReservationCancelledIE implements ReservationCancelledIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "conversation.ReservationCancelled";

    private final ProcessedMessageStore processedMessages;
    private final ReservationChatWindowRepository windows;

    public HandleReservationCancelledIE(ProcessedMessageStore processedMessages,
            ReservationChatWindowRepository windows) {
        this.processedMessages = processedMessages;
        this.windows = windows;
    }

    @Override
    public void handle(ReservationCancelledIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ReservationId reservationId = new ReservationId(event.reservationId());
        ReservationChatWindow window = windows.findByReservationId(reservationId)
                .orElseGet(() -> ReservationChatWindow.open(reservationId, new CustomerId(event.customerId()),
                        new ProviderId(event.providerId()), event.protectionWindowEnd()));
        windows.save(window.cancelled(event.cancelledAt(), event.checkInStart()));
    }
}
