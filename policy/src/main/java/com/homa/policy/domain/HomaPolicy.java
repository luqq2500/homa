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
import com.homa.policy.domain.exception.CancellationFeeAboveNoShowCeilingException;
import com.homa.policy.domain.exception.FlexibleTierMustBeFreeException;
import com.homa.policy.domain.exception.NoShowFineBelowLateCancellationFineException;
import com.homa.policy.domain.exception.PolicyChangeBeforeCurrentVersionException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The Homa-set values in force. Homa changes one value at a time; each change makes a new version and
 * leaves earlier versions untouched, so every reservation keeps the values that applied when it was
 * requested.
 */
public final class HomaPolicy {

    private static final List<PolicyValue> LAUNCH_VALUES = List.of(
            new ServiceFee(Percentage.of(15)),
            new DurationRange(1, 6),
            new HelpersPerSearch(1, 10),
            new MinimumNotice(Duration.ofHours(2)),
            new BookingHorizon(Duration.ofDays(30)),
            new RequestExpiry(Duration.ofHours(24)),
            new FeeWindow(Duration.ofHours(24)),
            new CancellationTierFee(CancellationTier.FLEXIBLE, CancellationTier.FLEXIBLE.launchFee()),
            new CancellationTierFee(CancellationTier.STANDARD, CancellationTier.STANDARD.launchFee()),
            new CancellationTierFee(CancellationTier.FIRM, CancellationTier.FIRM.launchFee()),
            new ProviderLateCancellationFine(Percentage.of(20)),
            new ProviderNoShowFine(Percentage.of(30)),
            new FineDeductionCap(Percentage.of(50)),
            new EarliestCheckIn(Duration.ofMinutes(15)),
            new CheckInPeriod(Duration.ofMinutes(15)),
            new CheckInRadius(300),
            new CustomerNoShowWait(Duration.ofMinutes(15)),
            new CustomerNoShowFee(Percentage.of(30)),
            new NoShowReportWindow(Duration.ofHours(1)),
            new CompletionReminder(Duration.ofMinutes(30)),
            new CompletionDeadline(Duration.ofHours(24)),
            new DisputeWindowExtension(Duration.ofHours(1)),
            new EvidenceRetention(Duration.ofDays(7)));

    private final PolicySnapshot current;

    private HomaPolicy(PolicySnapshot current) {
        this.current = current;
    }

    /** Version 1: the launch values, in force from {@code now}. */
    public static HomaPolicy initial(Instant now) {
        return new HomaPolicy(PolicySnapshot.first(Objects.requireNonNull(now), LAUNCH_VALUES));
    }

    /**
     * A new version with {@code value} changed, in force from {@code now}. This policy is left as it was.
     */
    public HomaPolicy change(PolicyValue value, Instant now) {
        if (now.isBefore(current.effectiveFrom())) {
            throw new PolicyChangeBeforeCurrentVersionException(now, current.effectiveFrom());
        }
        PolicySnapshot next = current.next(Objects.requireNonNull(value), now);
        requireFlexibleIsFree(next);
        requireCancellationFeesWithinNoShowCeiling(next);
        requireNoShowFineIsTheHighestFine(next);
        return new HomaPolicy(next);
    }

    public PolicySnapshot snapshot() {
        return current;
    }

    public int version() {
        return current.version();
    }

    private static void requireFlexibleIsFree(PolicySnapshot values) {
        Percentage flexible = values.cancellationFee(CancellationTier.FLEXIBLE);
        if (flexible.isGreaterThan(Percentage.ZERO)) {
            throw new FlexibleTierMustBeFreeException(flexible);
        }
    }

    /**
     * The customer no-show fee is the travel fee plus a share of the hours fee, so a tier whose share is
     * no larger can never charge more than a no-show. With the travel fee included in the price both
     * apply to the same provider price, so the rule still holds.
     */
    private static void requireCancellationFeesWithinNoShowCeiling(PolicySnapshot values) {
        Percentage ceiling = values.customerNoShowFee();
        for (CancellationTier tier : CancellationTier.values()) {
            Percentage fee = values.cancellationFee(tier);
            if (fee.isGreaterThan(ceiling)) {
                throw new CancellationFeeAboveNoShowCeilingException(tier.name(), fee, ceiling);
            }
        }
    }

    private static void requireNoShowFineIsTheHighestFine(PolicySnapshot values) {
        Percentage noShow = values.providerNoShowFine();
        Percentage late = values.providerLateCancellationFine();
        if (late.isGreaterThan(noShow)) {
            throw new NoShowFineBelowLateCancellationFineException(noShow, late);
        }
    }
}
