package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import kernel.Money;

/** A customer requested a reservation and the provider must answer. */
public record ReservationRequestedIE(
        String reservationId,
        String customerId,
        String providerId,
        Optional<String> enquiryId,
        String paymentMethod,
        Money total,
        Instant start,
        Instant end,
        Instant requestExpiresAt,
        Instant occurredAt) implements ReservationIE {

    public ReservationRequestedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(enquiryId, "enquiryId");
        Objects.requireNonNull(paymentMethod, "paymentMethod");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(requestExpiresAt, "requestExpiresAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
