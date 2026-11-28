package com.homa.provider.usecase.ie;

import com.homa.ie.events.RosterChangedIE;
import com.homa.provider.domain.ReservationId;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.usecase.port.in.RosterChangedIEHandler;
import com.homa.provider.usecase.port.out.ProcessedMessageStore;
import com.homa.provider.usecase.port.out.RosteredWindowsRepository;
import java.util.List;
import java.util.Objects;

/**
 * The provider set or changed a reservation's roster (ROS-1): the members listed are the ones rostered on it, in place
 * of the ones before, and each has the reservation's window (HLP-9). A member no longer listed has none. Provider
 * publishes nothing.
 *
 * <p>The event carries only the member ids, so the window comes from {@code ReservationReservedIE}, which Reservation
 * publishes first, in order with the reservation's other events. A roster for a reservation Provider holds no window
 * for, because it is already Completed or Cancelled or its Reserved event never arrived, has nothing to attach to: it
 * is recorded as handled and dropped, so the relay doesn't retry it forever.
 */
public final class HandleRosterChangedIE implements RosterChangedIEHandler {

    static final String HANDLER = "provider.RosterChanged";

    private final ProcessedMessageStore processedMessages;
    private final RosteredWindowsRepository rosteredWindows;

    public HandleRosterChangedIE(ProcessedMessageStore processedMessages, RosteredWindowsRepository rosteredWindows) {
        this.processedMessages = Objects.requireNonNull(processedMessages, "processedMessages");
        this.rosteredWindows = Objects.requireNonNull(rosteredWindows, "rosteredWindows");
    }

    @Override
    public void handle(RosterChangedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        List<TeamMemberId> roster = event.memberIds().stream().map(TeamMemberId::new).toList();
        rosteredWindows.findByReservationId(new ReservationId(event.reservationId()))
                .ifPresent(reservation -> rosteredWindows.save(reservation.rosteredOn(roster)));
    }
}
