package ie.events;

import ie.port.PolicyIE;
import java.time.Duration;
import java.time.Instant;
import kernel.Percentage;

/** Homa's values as they now stand, after a change or when announced at start-up. They apply to new reservations only. */
public record HomaValuesChangedIE(
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
        Duration evidenceRetentionPeriod,
        Duration disputeWindowExtension,
        Duration feeWindow,
        Percentage standardCancellationFee,
        Percentage firmCancellationFee,
        Percentage providerLateCancellationFine,
        Percentage providerNoShowFine,
        Percentage fineDeductionCap,
        Duration ratingWindow,
        int minimumRatingsToShow,
        Instant occurredAt) implements PolicyIE {
}
