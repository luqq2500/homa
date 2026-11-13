package policy.domain;

import static policy.domain.HomaValue.CUSTOMER_NO_SHOW_FEE;
import static policy.domain.HomaValue.FIRM_CANCELLATION_FEE;
import static policy.domain.HomaValue.MAXIMUM_HOURS;
import static policy.domain.HomaValue.MINIMUM_HOURS;
import static policy.domain.HomaValue.MINIMUM_NOTICE;
import static policy.domain.HomaValue.PROVIDER_LATE_CANCELLATION_FINE;
import static policy.domain.HomaValue.PROVIDER_NO_SHOW_FINE;
import static policy.domain.HomaValue.RESPONSE_DEADLINE;
import static policy.domain.HomaValue.STANDARD_CANCELLATION_FEE;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import kernel.Percentage;
import policy.domain.exception.HomaValueOutOfRangeException;
import policy.domain.exception.HomaValueRuleBrokenException;
import policy.domain.exception.HomaValueUnchangedException;

/**
 * One version of every value Homa sets. A change makes the next version; reservations keep the
 * version that was in force when they were requested.
 */
public final class HomaValues {

    private final long version;
    private final Map<HomaValue, Long> amounts;

    private HomaValues(long version, Map<HomaValue, Long> amounts) {
        this.version = version;
        this.amounts = new EnumMap<>(amounts);
        for (HomaValue value : HomaValue.values()) {
            checkAmount(value, this.amounts.get(value));
        }
        checkRules();
    }

    /** The values Homa launches with. */
    public static HomaValues launch() {
        Map<HomaValue, Long> amounts = new EnumMap<>(HomaValue.class);
        for (HomaValue value : HomaValue.values()) {
            amounts.put(value, value.launchAmount());
        }
        return new HomaValues(1, amounts);
    }

    /** A stored version, with an amount for every value. */
    public static HomaValues of(long version, Map<HomaValue, Long> amounts) {
        return new HomaValues(version, amounts);
    }

    public long version() {
        return version;
    }

    public long amountOf(HomaValue value) {
        return amounts.get(value);
    }

    public Percentage percentageOf(HomaValue value) {
        requireUnit(value, Unit.PERCENT);
        return Percentage.of((int) amountOf(value));
    }

    public Duration durationOf(HomaValue value) {
        requireUnit(value, Unit.MINUTES);
        return Duration.ofMinutes(amountOf(value));
    }

    /** The share of the hours fee a customer pays for cancelling inside the fee window. Flexible is always free. */
    public Percentage cancellationFeeOf(CancellationTier tier) {
        return switch (tier) {
            case FLEXIBLE -> Percentage.ZERO;
            case STANDARD -> percentageOf(STANDARD_CANCELLATION_FEE);
            case FIRM -> percentageOf(FIRM_CANCELLATION_FEE);
        };
    }

    /** The next version, with one value changed. */
    public HomaValues changed(HomaValue value, long amount) {
        if (amountOf(value) == amount) {
            throw new HomaValueUnchangedException(value);
        }
        Map<HomaValue, Long> next = new EnumMap<>(amounts);
        next.put(value, amount);
        return new HomaValues(version + 1, next);
    }

    private static void checkAmount(HomaValue value, Long amount) {
        if (amount == null || !value.unit().accepts(amount)) {
            throw new HomaValueOutOfRangeException(value, amount);
        }
    }

    private void checkRules() {
        require(amountOf(MINIMUM_HOURS) <= amountOf(MAXIMUM_HOURS),
                "The minimum hours cannot be more than the maximum hours");
        require(amountOf(RESPONSE_DEADLINE) < amountOf(MINIMUM_NOTICE),
                "The response deadline is always shorter than the minimum notice");
        checkFeeAndFineCeilings();
    }

    private void checkFeeAndFineCeilings() {
        require(amountOf(STANDARD_CANCELLATION_FEE) <= amountOf(CUSTOMER_NO_SHOW_FEE),
                "The Standard cancellation fee cannot be higher than the customer no-show fee");
        require(amountOf(FIRM_CANCELLATION_FEE) <= amountOf(CUSTOMER_NO_SHOW_FEE),
                "The Firm cancellation fee cannot be higher than the customer no-show fee");
        require(amountOf(PROVIDER_LATE_CANCELLATION_FINE) <= amountOf(PROVIDER_NO_SHOW_FINE),
                "The provider no-show fine is the highest provider fine");
    }

    private static void require(boolean holds, String rule) {
        if (!holds) {
            throw new HomaValueRuleBrokenException(rule);
        }
    }

    private static void requireUnit(HomaValue value, Unit unit) {
        if (value.unit() != unit) {
            throw new IllegalArgumentException(value + " is not measured in " + unit);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof HomaValues that && version == that.version && amounts.equals(that.amounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, amounts);
    }
}
