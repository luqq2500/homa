package policy.usecase;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;
import kernel.Percentage;
import policy.domain.CancellationTier;
import policy.domain.PolicySnapshot;
import policy.domain.PolicyValue;
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
import policy.usecase.exception.UnknownPolicyValueException;

/**
 * The name the admin console changes each Homa-set value by, in the order the spec lists them. It turns
 * the console's amount into the domain's value, and the value in force back into an amount. The domain
 * still checks every value's shape and the rules across values.
 */
enum PolicyValueName {
    SERVICE_FEE(Kind.PERCENTAGE, amount -> {
        Percentage p = PolicyAmounts.percentage("SERVICE_FEE", amount);
        return new ServiceFee(p);
    }, s -> PolicyAmounts.write(s.serviceFee())),
    DURATION_RANGE(Kind.RANGE, amount -> {
        int[] r = PolicyAmounts.range("DURATION_RANGE", amount);
        return new DurationRange(r[0], r[1]);
    }, s -> PolicyAmounts.range(s.durationRange().minHours(), s.durationRange().maxHours())),
    MINIMUM_NOTICE(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("MINIMUM_NOTICE", amount);
        return new MinimumNotice(d);
    }, s -> PolicyAmounts.write(s.minimumNotice())),
    BOOKING_HORIZON(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("BOOKING_HORIZON", amount);
        return new BookingHorizon(d);
    }, s -> PolicyAmounts.write(s.bookingHorizon())),
    REQUEST_EXPIRY(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("REQUEST_EXPIRY", amount);
        return new RequestExpiry(d);
    }, s -> PolicyAmounts.write(s.requestExpiry())),
    RESPONSE_DEADLINE(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("RESPONSE_DEADLINE", amount);
        return new ResponseDeadline(d);
    }, s -> PolicyAmounts.write(s.responseDeadline())),
    FEE_WINDOW(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("FEE_WINDOW", amount);
        return new FeeWindow(d);
    }, s -> PolicyAmounts.write(s.feeWindow())),
    FLEXIBLE_CANCELLATION_FEE(Kind.PERCENTAGE, amount -> {
        Percentage p = PolicyAmounts.percentage("FLEXIBLE_CANCELLATION_FEE", amount);
        return new CancellationTierFee(CancellationTier.FLEXIBLE, p);
    }, s -> PolicyAmounts.write(s.cancellationFee(CancellationTier.FLEXIBLE))),
    STANDARD_CANCELLATION_FEE(Kind.PERCENTAGE, amount -> {
        Percentage p = PolicyAmounts.percentage("STANDARD_CANCELLATION_FEE", amount);
        return new CancellationTierFee(CancellationTier.STANDARD, p);
    }, s -> PolicyAmounts.write(s.cancellationFee(CancellationTier.STANDARD))),
    FIRM_CANCELLATION_FEE(Kind.PERCENTAGE, amount -> {
        Percentage p = PolicyAmounts.percentage("FIRM_CANCELLATION_FEE", amount);
        return new CancellationTierFee(CancellationTier.FIRM, p);
    }, s -> PolicyAmounts.write(s.cancellationFee(CancellationTier.FIRM))),
    PROVIDER_LATE_CANCELLATION_FINE(Kind.PERCENTAGE, amount -> {
        Percentage p = PolicyAmounts.percentage("PROVIDER_LATE_CANCELLATION_FINE", amount);
        return new ProviderLateCancellationFine(p);
    }, s -> PolicyAmounts.write(s.providerLateCancellationFine())),
    PROVIDER_NO_SHOW_FINE(Kind.PERCENTAGE, amount -> {
        Percentage p = PolicyAmounts.percentage("PROVIDER_NO_SHOW_FINE", amount);
        return new ProviderNoShowFine(p);
    }, s -> PolicyAmounts.write(s.providerNoShowFine())),
    FINE_DEDUCTION_CAP(Kind.PERCENTAGE, amount -> {
        Percentage p = PolicyAmounts.percentage("FINE_DEDUCTION_CAP", amount);
        return new FineDeductionCap(p);
    }, s -> PolicyAmounts.write(s.fineDeductionCap())),
    TEAM_REMINDER(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("TEAM_REMINDER", amount);
        return new TeamReminder(d);
    }, s -> PolicyAmounts.write(s.teamReminder())),
    EARLIEST_CHECK_IN(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("EARLIEST_CHECK_IN", amount);
        return new EarliestCheckIn(d);
    }, s -> PolicyAmounts.write(s.earliestCheckIn())),
    CHECK_IN_PERIOD(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("CHECK_IN_PERIOD", amount);
        return new CheckInPeriod(d);
    }, s -> PolicyAmounts.write(s.checkInPeriod())),
    CHECK_IN_RADIUS(Kind.COUNT, amount -> {
        Integer n = PolicyAmounts.count("CHECK_IN_RADIUS", amount);
        return new CheckInRadius(n);
    }, s -> PolicyAmounts.write(s.checkInRadiusMetres())),
    CUSTOMER_NO_SHOW_WAIT(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("CUSTOMER_NO_SHOW_WAIT", amount);
        return new CustomerNoShowWait(d);
    }, s -> PolicyAmounts.write(s.customerNoShowWait())),
    CUSTOMER_NO_SHOW_FEE(Kind.PERCENTAGE, amount -> {
        Percentage p = PolicyAmounts.percentage("CUSTOMER_NO_SHOW_FEE", amount);
        return new CustomerNoShowFee(p);
    }, s -> PolicyAmounts.write(s.customerNoShowFee())),
    NO_SHOW_REPORT_WINDOW(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("NO_SHOW_REPORT_WINDOW", amount);
        return new NoShowReportWindow(d);
    }, s -> PolicyAmounts.write(s.noShowReportWindow())),
    COMPLETION_REMINDER(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("COMPLETION_REMINDER", amount);
        return new CompletionReminder(d);
    }, s -> PolicyAmounts.write(s.completionReminder())),
    COMPLETION_DEADLINE(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("COMPLETION_DEADLINE", amount);
        return new CompletionDeadline(d);
    }, s -> PolicyAmounts.write(s.completionDeadline())),
    DISPUTE_WINDOW_EXTENSION(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("DISPUTE_WINDOW_EXTENSION", amount);
        return new DisputeWindowExtension(d);
    }, s -> PolicyAmounts.write(s.disputeWindowExtension())),
    EVIDENCE_RETENTION(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("EVIDENCE_RETENTION", amount);
        return new EvidenceRetention(d);
    }, s -> PolicyAmounts.write(s.evidenceRetention())),
    RATING_WINDOW(Kind.PERIOD, amount -> {
        Duration d = PolicyAmounts.period("RATING_WINDOW", amount);
        return new RatingWindow(d);
    }, s -> PolicyAmounts.write(s.ratingWindow())),
    MINIMUM_RATINGS_TO_SHOW(Kind.COUNT, amount -> {
        Integer n = PolicyAmounts.count("MINIMUM_RATINGS_TO_SHOW", amount);
        return new MinimumRatingsToShow(n);
    }, s -> PolicyAmounts.write(s.minimumRatingsToShow()));

    /** How an amount of this value is written. */
    enum Kind { PERCENTAGE, PERIOD, COUNT, RANGE }

    private final Kind kind;
    private final Function<String, PolicyValue> fromAmount;
    private final Function<PolicySnapshot, String> amountIn;

    PolicyValueName(Kind kind, Function<String, PolicyValue> fromAmount, Function<PolicySnapshot, String> amountIn) {
        this.kind = kind;
        this.fromAmount = fromAmount;
        this.amountIn = amountIn;
    }

    static PolicyValueName named(String value) {
        return Arrays.stream(values())
                .filter(name -> name.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new UnknownPolicyValueException(value));
    }

    Kind kind() {
        return kind;
    }

    /** The domain value for {@code amount}, written as this value's kind is. */
    PolicyValue toValue(String amount) {
        return fromAmount.apply(amount);
    }

    /** This value's amount in {@code snapshot}, written as the console shows it. */
    String amountIn(PolicySnapshot snapshot) {
        return amountIn.apply(snapshot);
    }
}
