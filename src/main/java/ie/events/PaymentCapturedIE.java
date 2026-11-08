package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** A card was charged on acceptance. */
public record PaymentCapturedIE(
        String paymentId,
        String reservationId,
        Money amount,
        Instant occurredAt) implements PaymentIE {

    public PaymentCapturedIE {
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
