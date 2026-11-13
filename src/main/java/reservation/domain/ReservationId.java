package reservation.domain;

import java.util.UUID;

/** Generates the id of a reservation. */
public final class ReservationId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
