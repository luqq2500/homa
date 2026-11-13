package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;

/** The provider's business was seriously flagged: the job is on hold while Homa checks. */
public record ReservationDisputedForFlagIE(String reservationId, String customerId, String providerId, Instant start, Instant occurredAt) implements ReservationIE {}
