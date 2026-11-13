package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import kernel.Money;

/** A provider is fined for a late cancellation or a no-show. */
public record ProviderFinedIE(String reservationId, String providerId, Money amount, String reason, Instant occurredAt) implements ReservationIE {}
