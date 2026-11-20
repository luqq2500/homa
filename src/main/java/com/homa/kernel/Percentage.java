package com.homa.kernel;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A percentage from 0 to 100, kept exact. {@link #of(Money)} applies it to an amount without rounding, and
 * {@link #complement()} gives 100 − p, used for a discount (Hours Fee × (1 − 10%)).
 */
public final class Percentage implements Comparable<Percentage> {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    public static final Percentage ZERO = new Percentage(BigDecimal.ZERO);
    public static final Percentage HUNDRED_PERCENT = new Percentage(HUNDRED);

    private final BigDecimal value;

    private Percentage(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        if (value.signum() < 0 || value.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("A percentage must be from 0 to 100, was " + value.toPlainString());
        }
        this.value = value;
    }

    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }

    /** From a decimal string such as "10" or "15.5". */
    public static Percentage of(String value) {
        return new Percentage(new BigDecimal(value));
    }

    public static Percentage of(long value) {
        return new Percentage(BigDecimal.valueOf(value));
    }

    public BigDecimal value() {
        return value;
    }

    /** This percentage as a fraction: 15.5% is 0.155. Exact. */
    public BigDecimal asFraction() {
        return value.movePointLeft(2);
    }

    /** This percentage of the amount, exact: 15.5% of RM200 is RM31. */
    public Money of(Money amount) {
        return amount.times(asFraction());
    }

    /** 100 − this: the complement of a 10% discount is 90%. */
    public Percentage complement() {
        return new Percentage(HUNDRED.subtract(value));
    }

    public boolean isZero() {
        return value.signum() == 0;
    }

    @Override
    public int compareTo(Percentage other) {
        return value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Percentage other && value.compareTo(other.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.stripTrailingZeros().toPlainString() + "%";
    }
}
