package ie.port;

import java.time.Instant;

/** An integration event published by reservation: enquiries, the reservation lifecycle, settlements and fines. */
public interface ReservationIE {

    /** When the event happened. */
    Instant occurredAt();
}
