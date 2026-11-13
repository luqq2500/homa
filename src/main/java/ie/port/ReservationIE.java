package ie.port;

import java.time.Instant;

/** An integration event published by the reservation context. */
public interface ReservationIE {

    Instant occurredAt();
}
