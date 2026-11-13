package reservation.domain;

import kernel.Money;

/** A fine a provider pays Homa for cancelling late or not showing. */
public record Fine(Money amount, FineReason reason) {
}
