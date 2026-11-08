package com.homa.policy.domain;

import com.homa.kernel.Percentage;
import com.homa.policy.domain.PolicyValue.BookingHorizon;
import com.homa.policy.domain.PolicyValue.CancellationTierFee;
import com.homa.policy.domain.PolicyValue.CheckInPeriod;
import com.homa.policy.domain.PolicyValue.CheckInRadius;
import com.homa.policy.domain.PolicyValue.CompletionDeadline;
import com.homa.policy.domain.PolicyValue.CompletionReminder;
import com.homa.policy.domain.PolicyValue.CustomerNoShowFee;
import com.homa.policy.domain.PolicyValue.CustomerNoShowWait;
import com.homa.policy.domain.PolicyValue.DisputeWindowExtension;
import com.homa.policy.domain.PolicyValue.DurationRange;
import com.homa.policy.domain.PolicyValue.EarliestCheckIn;
import com.homa.policy.domain.PolicyValue.EvidenceRetention;
import com.homa.policy.domain.PolicyValue.FeeWindow;
import com.homa.policy.domain.PolicyValue.FineDeductionCap;
import com.homa.policy.domain.PolicyValue.HelpersPerSearch;
import com.homa.policy.domain.PolicyValue.MinimumNotice;
import com.homa.policy.domain.PolicyValue.NoShowReportWindow;
import com.homa.policy.domain.PolicyValue.ProviderLateCancellationFine;
import com.homa.policy.domain.PolicyValue.ProviderNoShowFine;
import com.homa.policy.domain.PolicyValue.RequestExpiry;
import com.homa.policy.domain.PolicyValue.ServiceFee;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public HelpersPerSearch helpersPerSearch() {
        return value(HelpersPerSearch.class);
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

    public Duration earliestCheckIn() {
        return value(EarliestCheckIn.class).beforeStart();
    }

    public Duration checkInPeriod() {
        return value(CheckInPeriod.class).afterStart();
    }

    public int checkInRadiusMetres() {
        return value(CheckInRadius.class).metres();
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
