package reservation.domain;

import java.util.UUID;

/** Type-safe generator for Reservation ids. */
public final class ReservationId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
