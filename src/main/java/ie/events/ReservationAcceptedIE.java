package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** A request was accepted and now holds the provider's time. */
public record ReservationAcceptedIE(String reservationId, String customerId, String providerId, Instant start, Instant end, boolean instantBook, Instant occurredAt) implements ReservationIE {}
