package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** The flag was cleared in time: the reservation goes ahead as booked. */
public record ReservationRestoredIE(String reservationId, String customerId, String providerId, Instant occurredAt) implements ReservationIE {}
