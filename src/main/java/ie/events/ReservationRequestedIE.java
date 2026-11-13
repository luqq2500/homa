package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import kernel.Money;
import kernel.Percentage;

/** A customer submitted a request; its payment is to be secured. The enquiry id is null for a direct request. */
public record ReservationRequestedIE(String reservationId, String customerId, String providerId, String enquiryId, String paymentMethod, Money providerPrice, Money serviceFee, Money total, Percentage serviceFeeRate, Instant start, Instant end, Instant occurredAt) implements ReservationIE {}
