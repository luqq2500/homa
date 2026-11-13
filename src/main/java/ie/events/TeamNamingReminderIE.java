package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** Homa reminds the provider to name the team. */
public record TeamNamingReminderIE(String reservationId, String providerId, Instant occurredAt) implements ReservationIE {}
