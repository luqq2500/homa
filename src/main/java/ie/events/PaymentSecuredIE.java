package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;

/** The card is authorised or the upfront payment is paid for the request's current total. */
public record PaymentSecuredIE(String reservationId, Instant occurredAt) implements PaymentIE {}
