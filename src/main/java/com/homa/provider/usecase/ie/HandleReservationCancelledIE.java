package com.homa.provider.usecase.ie;

import com.homa.ie.events.ReservationCancelledIE;
import com.homa.provider.domain.ReservationId;
import com.homa.provider.usecase.port.in.ReservationCancelledIEHandler;
import com.homa.provider.usecase.port.out.ProcessedMessageStore;
import com.homa.provider.usecase.port.out.RosteredWindowsRepository;
import java.util.Objects;

/**
 * A Reserved reservation is Cancelled, for any cause: its window is dropped, so a member rostered on it can be
 * removed from then on (HLP-9). A reservation Provider holds no window for has nothing to drop: the event is recorded
 * as handled and changes nothing, so the relay doesn't retry it forever. Provider publishes nothing.
 */
public final class HandleReservationCancelledIE implements ReservationCancelledIEHandler {

    static final String HANDLER = "provider.ReservationCancelled";

    private final ProcessedMessageStore processedMessages;
    private final RosteredWindowsRepository rosteredWindows;

    public HandleReservationCancelledIE(
            ProcessedMessageStore processedMessages, RosteredWindowsRepository rosteredWindows) {
        this.processedMessages = Objects.requireNonNull(processedMessages, "processedMessages");
        this.rosteredWindows = Objects.requireNonNull(rosteredWindows, "rosteredWindows");
    }

    @Override
    public void handle(ReservationCancelledIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        rosteredWindows.delete(new ReservationId(event.reservationId()));
    }
}
