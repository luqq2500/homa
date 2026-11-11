package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;
import java.util.Objects;

/** A payment failed when a request was made: the card was declined, or the FPX payment failed or was abandoned. Nothing was charged, and the request ends. */
public record PaymentFailedIE(
        String reservationId,
        String paymentMethod,
        Instant occurredAt) implements PaymentIE {

    public PaymentFailedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(paymentMethod, "paymentMethod");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
