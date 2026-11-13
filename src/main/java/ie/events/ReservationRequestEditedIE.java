package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import kernel.Money;

/** A customer edited a request; a changed total is to be secured again. */
public record ReservationRequestEditedIE(String reservationId, String customerId, String providerId, Money providerPrice, Money serviceFee, Money total, Instant start, Instant end, Instant occurredAt) implements ReservationIE {}
