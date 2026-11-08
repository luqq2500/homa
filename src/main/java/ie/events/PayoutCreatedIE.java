package ie.events;

import ie.port.PaymentIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** A provider's payout for a reservation, after any fine deductions. */
public record PayoutCreatedIE(
        String payoutId,
        String providerId,
        String reservationId,
        Money gross,
        Money deducted,
        Money net,
        Instant occurredAt) implements PaymentIE {

    public PayoutCreatedIE {
        Objects.requireNonNull(payoutId, "payoutId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(gross, "gross");
        Objects.requireNonNull(deducted, "deducted");
        Objects.requireNonNull(net, "net");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
