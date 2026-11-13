package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;

/** The card was declined, or the upfront payment failed or was abandoned. */
public record PaymentFailedIE(String reservationId, Instant occurredAt) implements PaymentIE {}
