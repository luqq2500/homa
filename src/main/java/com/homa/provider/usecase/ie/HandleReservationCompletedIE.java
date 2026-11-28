package com.homa.provider.usecase.ie;

import com.homa.ie.events.ReservationCompletedIE;
import com.homa.provider.domain.ReservationId;
import com.homa.provider.usecase.port.in.ReservationCompletedIEHandler;
import com.homa.provider.usecase.port.out.ProcessedMessageStore;
import com.homa.provider.usecase.port.out.RosteredWindowsRepository;
import java.util.Objects;

/**
 * A reservation is Completed: its window is dropped, so a member rostered on it can be removed from then on (HLP-9).
 * A reservation Provider holds no window for has nothing to drop: the event is recorded as handled and changes
 * nothing, so the relay doesn't retry it forever. Provider publishes nothing.
 */
public final class HandleReservationCompletedIE implements ReservationCompletedIEHandler {

    static final String HANDLER = "provider.ReservationCompleted";

    private final ProcessedMessageStore processedMessages;
    private final RosteredWindowsRepository rosteredWindows;

    public HandleReservationCompletedIE(
            ProcessedMessageStore processedMessages, RosteredWindowsRepository rosteredWindows) {
        this.processedMessages = Objects.requireNonNull(processedMessages, "processedMessages");
        this.rosteredWindows = Objects.requireNonNull(rosteredWindows, "rosteredWindows");
    }

    @Override
    public void handle(ReservationCompletedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        rosteredWindows.delete(new ReservationId(event.reservationId()));
    }
}
