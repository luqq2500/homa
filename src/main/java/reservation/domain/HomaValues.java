package reservation.domain;

import java.time.Duration;
import kernel.Percentage;

/**
 * The Homa-set values a reservation uses. Each reservation keeps the values that were in force when
 * it was requested; a later change applies to new reservations only.
 */
public record HomaValues(
        long version,
        Percentage serviceFee,
        int minimumHours,
        int maximumHours,
        Duration minimumNotice,
        Duration bookingHorizon,
        Duration teamReminderTime,
        Duration earliestCheckIn,
        Duration checkInPeriod,
        int checkInRadiusMetres,
        Duration customerNoShowWait,
        Percentage customerNoShowFee,
        Duration noShowReportWindow,
        Duration requestExpiryTime,
        Duration responseDeadline,
        Duration completionReminderTime,
        Duration completionDeadline,
        Duration disputeWindowExtension,
        Duration feeWindow,
        Percentage standardCancellationFee,
        Percentage firmCancellationFee,
        Percentage providerLateCancellationFine,
        Percentage providerNoShowFine) {

    /** The share of the hours fee a customer pays for cancelling inside the fee window. Flexible is always free. */
    public Percentage cancellationFeeOf(CancellationTier tier) {
        return switch (tier) {
            case FLEXIBLE -> Percentage.ZERO;
            case STANDARD -> standardCancellationFee;
            case FIRM -> firmCancellationFee;
        };
    }
}
