package reservation.domain;

import java.time.Instant;
import java.util.Objects;
import kernel.Money;
import kernel.Percentage;

/**
 * Who gets what when a reservation ends: the provider's earning, Homa's service fee and the customer's
 * refund, which always add up to the total paid. The service fee follows what the provider finally
 * earns, and the refund is worked out last so nothing is lost to rounding. The {@code payment} context
 * carries it out.
 *
 * @param releaseAt when the provider's earning is released
 */
public record Settlement(
        String reservationId,
        Ending ending,
        Money total,
        Money providerEarning,
        Money serviceFee,
        Money customerRefund,
        Instant releaseAt) {

    public Settlement {
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(ending);
        Objects.requireNonNull(releaseAt);
    }

    static Settlement of(
            String reservationId, Ending ending, Money total, Money providerEarning, Percentage serviceFeeShare, Instant releaseAt) {
        Money serviceFee = serviceFeeShare.of(providerEarning);
        Money refund = total.minus(providerEarning).minus(serviceFee);
        return new Settlement(reservationId, ending, total, providerEarning, serviceFee, refund, releaseAt);
    }
}
