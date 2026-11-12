package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** Payment for a request is secured: the card is authorised for {@code amount}, or that much is paid by FPX. The provider can accept once it covers the total. */
public record PaymentSecuredIE(
        String paymentId,
        String reservationId,
        Money amount,
        Instant occurredAt) implements PaymentIE {

    public PaymentSecuredIE {
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
