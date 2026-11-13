package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** Homa reminds the provider to submit completion. */
public record CompletionReminderIE(String reservationId, String providerId, Instant occurredAt) implements ReservationIE {}
