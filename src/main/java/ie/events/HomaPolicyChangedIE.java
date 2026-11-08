package ie.events;

import ie.port.PolicyIE;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import kernel.Percentage;

/** A new version of the Homa-set values took effect. It carries every value, so listeners keep a full copy. */
public record HomaPolicyChangedIE(
        int version,
        Instant effectiveFrom,
        Percentage serviceFee,
        int minHours,
        int maxHours,
        int minHelpers,
        int maxHelpers,
        Duration minimumNotice,
        Duration bookingHorizon,
        Duration requestExpiry,
        Duration feeWindow,
        Percentage flexibleCancellationFee,
        Percentage standardCancellationFee,
        Percentage firmCancellationFee,
        Percentage providerLateCancellationFine,
        Percentage providerNoShowFine,
        Percentage fineDeductionCap,
        Duration teamReminder,
        Duration earliestCheckIn,
        Duration checkInPeriod,
        int checkInRadiusMetres,
        Duration customerNoShowWait,
        Percentage customerNoShowFee,
        Duration noShowReportWindow,
        Duration completionReminder,
        Duration completionDeadline,
        Duration disputeWindowExtension,
        Duration evidenceRetention,
        Duration ratingWindow,
        int minimumRatingsToShow,
        int reverificationPeriodMonths,
        Duration reverificationReminder,
        Duration earlyRecheckDeadline,
        Instant occurredAt) implements PolicyIE {

    public HomaPolicyChangedIE {
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        Objects.requireNonNull(serviceFee, "serviceFee");
        Objects.requireNonNull(minimumNotice, "minimumNotice");
        Objects.requireNonNull(bookingHorizon, "bookingHorizon");
        Objects.requireNonNull(requestExpiry, "requestExpiry");
        Objects.requireNonNull(feeWindow, "feeWindow");
        Objects.requireNonNull(flexibleCancellationFee, "flexibleCancellationFee");
        Objects.requireNonNull(standardCancellationFee, "standardCancellationFee");
        Objects.requireNonNull(firmCancellationFee, "firmCancellationFee");
        Objects.requireNonNull(providerLateCancellationFine, "providerLateCancellationFine");
        Objects.requireNonNull(providerNoShowFine, "providerNoShowFine");
        Objects.requireNonNull(fineDeductionCap, "fineDeductionCap");
        Objects.requireNonNull(teamReminder, "teamReminder");
        Objects.requireNonNull(earliestCheckIn, "earliestCheckIn");
        Objects.requireNonNull(checkInPeriod, "checkInPeriod");
        Objects.requireNonNull(customerNoShowWait, "customerNoShowWait");
        Objects.requireNonNull(customerNoShowFee, "customerNoShowFee");
        Objects.requireNonNull(noShowReportWindow, "noShowReportWindow");
        Objects.requireNonNull(completionReminder, "completionReminder");
        Objects.requireNonNull(completionDeadline, "completionDeadline");
        Objects.requireNonNull(disputeWindowExtension, "disputeWindowExtension");
        Objects.requireNonNull(evidenceRetention, "evidenceRetention");
        Objects.requireNonNull(ratingWindow, "ratingWindow");
        Objects.requireNonNull(reverificationReminder, "reverificationReminder");
        Objects.requireNonNull(earlyRecheckDeadline, "earlyRecheckDeadline");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
