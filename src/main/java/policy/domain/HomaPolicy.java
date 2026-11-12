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
import policy.domain.PolicyValue.MinimumNotice;
import policy.domain.PolicyValue.MinimumRatingsToShow;
import policy.domain.PolicyValue.NoShowReportWindow;
import policy.domain.PolicyValue.ProviderLateCancellationFine;
import policy.domain.PolicyValue.ProviderNoShowFine;
import policy.domain.PolicyValue.RatingWindow;
import policy.domain.PolicyValue.RequestExpiry;
import policy.domain.PolicyValue.ResponseDeadline;
import policy.domain.PolicyValue.ServiceFee;
import policy.domain.PolicyValue.TeamReminder;
import policy.domain.exception.CancellationFeeAboveNoShowCeilingException;
import policy.domain.exception.FlexibleTierMustBeFreeException;
import policy.domain.exception.NoShowFineBelowLateCancellationFineException;
import policy.domain.exception.PolicyChangeBeforeCurrentVersionException;
import policy.domain.exception.ResponseDeadlineNotBeforeMinimumNoticeException;
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
            new MinimumNotice(Duration.ofHours(2)),
            new BookingHorizon(Duration.ofDays(30)),
            new RequestExpiry(Duration.ofHours(24)),
            new ResponseDeadline(Duration.ofHours(1)),
            new FeeWindow(Duration.ofHours(24)),
            new CancellationTierFee(CancellationTier.FLEXIBLE, CancellationTier.FLEXIBLE.launchFee()),
            new CancellationTierFee(CancellationTier.STANDARD, CancellationTier.STANDARD.launchFee()),
            new CancellationTierFee(CancellationTier.FIRM, CancellationTier.FIRM.launchFee()),
            new ProviderLateCancellationFine(Percentage.of(20)),
            new ProviderNoShowFine(Percentage.of(30)),
            new FineDeductionCap(Percentage.of(50)),
            new TeamReminder(Duration.ofHours(6)),
            new EarliestCheckIn(Duration.ofMinutes(15)),
            new CheckInPeriod(Duration.ofMinutes(15)),
            new CheckInRadius(300),
            new CustomerNoShowWait(Duration.ofMinutes(15)),
            new CustomerNoShowFee(Percentage.of(30)),
            new NoShowReportWindow(Duration.ofHours(1)),
            new CompletionReminder(Duration.ofMinutes(30)),
            new CompletionDeadline(Duration.ofHours(24)),
            new DisputeWindowExtension(Duration.ofHours(24)),
            new EvidenceRetention(Duration.ofDays(120)),
            new RatingWindow(Duration.ofDays(14)),
            new MinimumRatingsToShow(3));

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
        requireResponseDeadlineBeforeMinimumNotice(next);
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

    /** A request made at the minimum notice must still have time to be answered before its response deadline. */
    private static void requireResponseDeadlineBeforeMinimumNotice(PolicySnapshot values) {
        if (values.responseDeadline().compareTo(values.minimumNotice()) >= 0) {
            throw new ResponseDeadlineNotBeforeMinimumNoticeException(values.responseDeadline(), values.minimumNotice());
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
