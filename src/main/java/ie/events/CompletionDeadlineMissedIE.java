package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import kernel.Money;

/** The provider missed the completion deadline; Homa reviews the reservation. */
public record CompletionDeadlineMissedIE(String reservationId, String customerId, String providerId, Money atStake, Instant occurredAt) implements ReservationIE {}
