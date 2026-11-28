package com.homa.conversation.usecase.ie;

import com.homa.conversation.domain.CustomerId;
import com.homa.conversation.domain.ProviderId;
import com.homa.conversation.domain.ReservationChatWindow;
import com.homa.conversation.domain.ReservationId;
import com.homa.conversation.usecase.port.in.ReservationExpiredIEHandler;
import com.homa.conversation.usecase.port.out.ProcessedMessageStore;
import com.homa.conversation.usecase.port.out.ReservationChatWindowRepository;
import com.homa.ie.events.ReservationExpiredIE;

/**
 * CHT-5: the reservation passed its acceptance deadline unanswered, so its chat is read-only at once. Only the
 * reservation's chat window changes. An expiry heard before the request is still kept, as in
 * {@link HandleReservationWithdrawnIE}. Publishes nothing.
 */
public final class HandleReservationExpiredIE implements ReservationExpiredIEHandler {

    /** Stable: never derived from the class name. */
    static final String HANDLER = "conversation.ReservationExpired";

    private final ProcessedMessageStore processedMessages;
    private final ReservationChatWindowRepository windows;

    public HandleReservationExpiredIE(ProcessedMessageStore processedMessages,
            ReservationChatWindowRepository windows) {
        this.processedMessages = processedMessages;
        this.windows = windows;
    }

    @Override
    public void handle(ReservationExpiredIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ReservationId reservationId = new ReservationId(event.reservationId());
        windows.save(windows.findByReservationId(reservationId)
                .map(ReservationChatWindow::expired)
                .orElseGet(() -> ReservationChatWindow.expiredBeforeRequested(reservationId,
                        new CustomerId(event.customerId()), new ProviderId(event.providerId()))));
    }
}
