package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** Money went back to the customer's original payment method. */
public record PaymentRefundedIE(
        String paymentId,
        String reservationId,
        Money amount,
        Instant occurredAt) implements PaymentIE {

    public PaymentRefundedIE {
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
