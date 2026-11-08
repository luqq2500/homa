package reservation.domain;

import java.util.Objects;
import java.util.Optional;
import kernel.Money;

/** The outcome of a provider cancellation or no-show: the customer's full refund, and any fine. */
public record CancellationResult(Settlement settlement, Optional<FineCharge> fine) {

    public CancellationResult {
        Objects.requireNonNull(settlement);
        Objects.requireNonNull(fine);
    }

    /** The fine amount, or nothing when the provider is not fined. */
    public Money fineAmount() {
        return fine.map(FineCharge::amount).orElse(Money.ZERO);
    }
}
