package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;

/** A reservation's access notes changed, so the provider sees the new notes once the reservation is accepted. */
public record AccessNotesChangedIE(
        String reservationId,
        String notes,
        Instant occurredAt) implements ReservationIE {

    public AccessNotesChangedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(notes, "notes");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
