package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import kernel.Money;

/** The customer challenged a no-show; Homa investigates. */
public record NoShowChallengedIE(String reservationId, String customerId, String providerId, Money atStake, Instant occurredAt) implements ReservationIE {}
