package com.homa.provider.usecase.ie;

import com.homa.ie.events.ReservationReservedIE;
import com.homa.provider.domain.ReservationId;
import com.homa.provider.domain.RosteredReservation;
import com.homa.provider.usecase.port.in.ReservationReservedIEHandler;
import com.homa.provider.usecase.port.out.ProcessedMessageStore;
import com.homa.provider.usecase.port.out.RosteredWindowsRepository;
import java.util.Objects;

/**
 * A reservation is Reserved: Provider keeps its window, from the start of the Check-in Period to the job end, so that
 * a member rostered on it can't be removed inside it (HLP-9). Nobody is rostered yet: the provider sets the roster
 * after this ({@code RosterChangedIE}, ROS-1). Provider publishes nothing.
 */
public final class HandleReservationReservedIE implements ReservationReservedIEHandler {

    static final String HANDLER = "provider.ReservationReserved";

    private final ProcessedMessageStore processedMessages;
    private final RosteredWindowsRepository rosteredWindows;

    public HandleReservationReservedIE(
            ProcessedMessageStore processedMessages, RosteredWindowsRepository rosteredWindows) {
        this.processedMessages = Objects.requireNonNull(processedMessages, "processedMessages");
        this.rosteredWindows = Objects.requireNonNull(rosteredWindows, "rosteredWindows");
    }

    @Override
    public void handle(ReservationReservedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        rosteredWindows.save(RosteredReservation.reserved(
                new ReservationId(event.reservationId()), event.checkInStart(), event.jobEnd()));
    }
}
