package com.homa.conversation.usecase.ie;

import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.ProviderId;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.ReservationId;
import com.homa.conversation.usecase.port.in.ReservationWithdrawnIEHandler;
import com.homa.conversation.usecase.port.out.ProcessedMessageStore;
import com.homa.conversation.usecase.port.out.ReservationChatWindowRepository;
import com.homa.ie.events.ReservationWithdrawnIE;

/**
 * CHT-5: the customer withdrew the reservation before the provider accepted it, so its chat is read-only at once.
 * Only the reservation's chat window changes; the chat and its messages stay readable. Reservation's events about
 * one reservation arrive in order, but if the withdrawal is heard before the request, the end is still kept: the
 * window is created ended, and the request opens the chat read-only and fills in the Protection Window end. Publishes
 * nothing.
 */
public final class HandleReservationWithdrawnIE implements ReservationWithdrawnIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "conversation.ReservationWithdrawn";

    private final ProcessedMessageStore processedMessages;
    private final ReservationChatWindowRepository windows;

    public HandleReservationWithdrawnIE(ProcessedMessageStore processedMessages,
            ReservationChatWindowRepository windows) {
        this.processedMessages = processedMessages;
        this.windows = windows;
    }

    @Override
    public void handle(ReservationWithdrawnIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ReservationId reservationId = new ReservationId(event.reservationId());
        windows.save(windows.findByReservationId(reservationId)
                .map(ReservationChatWindow::withdrawn)
                .orElseGet(() -> ReservationChatWindow.withdrawnBeforeRequested(reservationId,
                        new CustomerId(event.customerId()), new ProviderId(event.providerId()))));
    }
}
