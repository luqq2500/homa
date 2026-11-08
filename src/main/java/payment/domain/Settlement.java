package payment.domain;

import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/**
 * Who gets what for a reservation, as decided by the {@code reservation} context: the provider's earning,
 * Homa's service fee and the customer's refund, and when the earning is released.
 */
public record Settlement(String reservationId, Money providerEarning, Money serviceFee, Money customerRefund, Instant releaseAt) {

    public Settlement {
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(providerEarning);
        Objects.requireNonNull(serviceFee);
        Objects.requireNonNull(customerRefund);
        Objects.requireNonNull(releaseAt);
    }

    Money total() {
        return providerEarning.plus(serviceFee).plus(customerRefund);
    }
}
