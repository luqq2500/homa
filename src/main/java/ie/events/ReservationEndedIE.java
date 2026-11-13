package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** A request or reservation ended without a completed job: declined, expired or cancelled, and how. */
public record ReservationEndedIE(String reservationId, String customerId, String providerId, String status, String ending, boolean wasReserved, Instant start, Instant end, Instant occurredAt) implements ReservationIE {}
