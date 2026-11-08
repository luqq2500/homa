package reservation.domain;

import java.util.Objects;
import kernel.Money;

/** A fine the provider owes Homa. The {@code payment} context deducts it from later payouts. */
public record FineCharge(String providerId, String reservationId, Money amount, FineReason reason) {

    public FineCharge {
        Objects.requireNonNull(providerId);
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(reason);
    }
}
