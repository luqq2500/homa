package ie.events;

import ie.port.ReservationIE;
import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** Who gets what for a reservation that ended. The parts add up to the total. */
public record SettlementDecidedIE(
        String reservationId,
        String providerId,
        String ending,
        Money total,
        Money providerEarning,
        Money serviceFee,
        Money customerRefund,
        Instant releaseAt,
        Instant occurredAt) implements ReservationIE {

    public SettlementDecidedIE {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(ending, "ending");
        Objects.requireNonNull(total, "total");
        Objects.requireNonNull(providerEarning, "providerEarning");
        Objects.requireNonNull(serviceFee, "serviceFee");
        Objects.requireNonNull(customerRefund, "customerRefund");
        Objects.requireNonNull(releaseAt, "releaseAt");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
