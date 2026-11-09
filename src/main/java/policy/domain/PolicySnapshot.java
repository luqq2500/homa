package policy.domain;

import kernel.Percentage;
import policy.domain.PolicyValue.BookingHorizon;
import policy.domain.PolicyValue.CancellationTierFee;
import policy.domain.PolicyValue.CheckInPeriod;
import policy.domain.PolicyValue.CheckInRadius;
import policy.domain.PolicyValue.CompletionDeadline;
import policy.domain.PolicyValue.CompletionReminder;
import policy.domain.PolicyValue.CustomerNoShowFee;
import policy.domain.PolicyValue.CustomerNoShowWait;
import policy.domain.PolicyValue.DisputeWindowExtension;
import policy.domain.PolicyValue.DurationRange;
import policy.domain.PolicyValue.EarliestCheckIn;
import policy.domain.PolicyValue.EvidenceRetention;
import policy.domain.PolicyValue.FeeWindow;
import policy.domain.PolicyValue.FineDeductionCap;
import policy.domain.PolicyValue.AcceptedDocuments;
import policy.domain.PolicyValue.FaceCheckAttempts;
import policy.domain.PolicyValue.VerificationLinkExpiry;
import policy.domain.PolicyValue.MinimumNotice;
import policy.domain.PolicyValue.MinimumRatingsToShow;
import policy.domain.PolicyValue.NoShowReportWindow;
import policy.domain.PolicyValue.ProviderLateCancellationFine;
import policy.domain.PolicyValue.ProviderNoShowFine;
import policy.domain.PolicyValue.RatingWindow;
import policy.domain.PolicyValue.RequestExpiry;
import policy.domain.PolicyValue.EarlyRecheckDeadline;
import policy.domain.PolicyValue.ReverificationPeriod;
import policy.domain.PolicyValue.ReverificationReminder;
import policy.domain.PolicyValue.ServiceFee;
import policy.domain.PolicyValue.TeamReminder;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An immutable copy of one version of the Homa-set values. A reservation keeps the snapshot that was
 * in force when it was requested, so a later change only affects new reservations.
 */
public final class PolicySnapshot {

    private final int version;
    private final Instant effectiveFrom;
    private final Map<Object, PolicyValue> values;

    private PolicySnapshot(int version, Instant effectiveFrom, Map<Object, PolicyValue> values) {
        this.version = version;
        this.effectiveFrom = effectiveFrom;
        this.values = Map.copyOf(values);
    }

    static PolicySnapshot first(Instant effectiveFrom, Collection<PolicyValue> values) {
        Map<Object, PolicyValue> byKey = new HashMap<>();
        values.forEach(value -> byKey.put(value.key(), value));
        return new PolicySnapshot(1, effectiveFrom, byKey);
    }

    /** The next version: the same values with {@code changed} in place of the value it replaces. */
    PolicySnapshot next(PolicyValue changed, Instant effectiveFrom) {
        Map<Object, PolicyValue> byKey = new HashMap<>(values);
        byKey.put(changed.key(), changed);
        return new PolicySnapshot(version + 1, effectiveFrom, byKey);
    }

    public int version() {
        return version;
    }

    public Instant effectiveFrom() {
        return effectiveFrom;
    }

    public Percentage serviceFee() {
        return value(ServiceFee.class).ofProviderPrice();
    }

    public DurationRange durationRange() {
        return value(DurationRange.class);
    }

    public Duration minimumNotice() {
        return value(MinimumNotice.class).period();
    }

    public Duration bookingHorizon() {
        return value(BookingHorizon.class).period();
    }

    public Duration requestExpiry() {
        return value(RequestExpiry.class).period();
    }

    public Duration feeWindow() {
        return value(FeeWindow.class).period();
    }

    /** The share of the hours fee this tier charges when the customer cancels within the fee window. */
    public Percentage cancellationFee(CancellationTier tier) {
        return ((CancellationTierFee) values.get(tier)).ofHoursFee();
    }

    public Percentage providerLateCancellationFine() {
        return value(ProviderLateCancellationFine.class).ofHoursFee();
    }

    public Percentage providerNoShowFine() {
        return value(ProviderNoShowFine.class).ofHoursFee();
    }

    public Percentage fineDeductionCap() {
        return value(FineDeductionCap.class).ofPayout();
    }

    public Duration teamReminder() {
        return value(TeamReminder.class).beforeStart();
    }

    public Duration earliestCheckIn() {
        return value(EarliestCheckIn.class).beforeStart();
    }

    public Duration checkInPeriod() {
        return value(CheckInPeriod.class).afterStart();
    }

    public int checkInRadiusMetres() {
        return value(CheckInRadius.class).metres();
    }

    public int faceCheckAttempts() {
        return value(FaceCheckAttempts.class).attempts();
    }

    public Duration verificationLinkExpiry() {
        return value(VerificationLinkExpiry.class).period();
    }

    public Set<AcceptedDocument> acceptedDocuments() {
        return value(AcceptedDocuments.class).documents();
    }

    public Duration customerNoShowWait() {
        return value(CustomerNoShowWait.class).period();
    }

    /** The customer no-show fee is the travel fee plus this share of the hours fee. */
    public Percentage customerNoShowFee() {
        return value(CustomerNoShowFee.class).ofHoursFee();
    }

    public Duration noShowReportWindow() {
        return value(NoShowReportWindow.class).period();
    }

    public Duration completionReminder() {
        return value(CompletionReminder.class).afterEnd();
    }

    public Duration completionDeadline() {
        return value(CompletionDeadline.class).afterEnd();
    }

    public Duration disputeWindowExtension() {
        return value(DisputeWindowExtension.class).period();
    }

    public Duration ratingWindow() {
        return value(RatingWindow.class).afterCompleted();
    }

    public int minimumRatingsToShow() {
        return value(MinimumRatingsToShow.class).ratings();
    }

    /** Re-verification is due this many months after each person's last successful verification. */
    public int reverificationPeriodMonths() {
        return value(ReverificationPeriod.class).months();
    }

    public Duration reverificationReminder() {
        return value(ReverificationReminder.class).beforeDue();
    }

    public Duration earlyRecheckDeadline() {
        return value(EarlyRecheckDeadline.class).afterRequest();
    }

    public Duration evidenceRetention() {
        return value(EvidenceRetention.class).period();
    }

    /** Every value in this version. */
    public List<PolicyValue> values() {
        return List.copyOf(values.values());
    }

    private <T extends PolicyValue> T value(Class<T> type) {
        return type.cast(values.get(type));
    }
}
