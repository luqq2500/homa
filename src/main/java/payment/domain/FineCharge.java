package payment.domain;

import java.util.Objects;
import kernel.Money;

/** A fine the {@code reservation} context decided: how much, why, and for which reservation. */
public record FineCharge(String reservationId, Money amount, FineReason reason) {

    public FineCharge {
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(reason);
    }
}
