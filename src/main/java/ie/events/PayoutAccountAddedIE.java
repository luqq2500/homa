package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;

/** A provider added the business bank account their payouts go to. */
public record PayoutAccountAddedIE(String providerId, Instant occurredAt) implements PaymentIE {}
