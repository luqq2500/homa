package reservation.domain;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import kernel.Percentage;

/**
 * The Homa-set values in force when the reservation was requested. The reservation keeps them, so a later
 * policy change affects new reservations only.
 */
public record PolicySnapshot(
        int version,
        Percentage serviceFee,
        Duration minimumNotice,
        Duration bookingHorizon,
        Duration requestExpiry,
        Duration feeWindow,
        Map<CancellationTier, Percentage> cancellationFees,
        Percentage providerLateCancellationFine,
        Percentage providerNoShowFine,
        Duration earliestCheckIn,
        Duration checkInPeriod,
        int checkInRadiusMetres,
        Duration customerNoShowWait,
        Percentage customerNoShowFee,
        Duration noShowReportWindow,
        Duration completionReminder,
        Duration completionDeadline,
        Duration disputeWindowExtension,
        Duration teamReminder) {

    public PolicySnapshot {
        cancellationFees = Map.copyOf(cancellationFees);
        for (CancellationTier tier : CancellationTier.values()) {
            Objects.requireNonNull(cancellationFees.get(tier), "No fee for the " + tier + " tier");
        }
    }

    /** The share of the hours fee the tier charges for a cancellation inside the fee window. */
    public Percentage cancellationFee(CancellationTier tier) {
        return cancellationFees.get(tier);
    }
}
