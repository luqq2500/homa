package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import kernel.Money;

/** How a reservation's money is split now: the provider receives this, the customer is refunded the rest, and the service fee follows. */
public record ReservationSettledIE(String reservationId, String providerId, Money providerReceives, String payoutKind, Instant occurredAt) implements ReservationIE {}
