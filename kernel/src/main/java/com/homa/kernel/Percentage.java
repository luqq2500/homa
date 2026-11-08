package com.homa.kernel;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A share from 0% to 100%, held in basis points (hundredths of a percent) so values such as 12.5% are
 * exact.
 *
 * <p>Applying a percentage to money rounds half-up to the nearest sen.
 */
public record Percentage(int basisPoints) implements Comparable<Percentage> {

    private static final int BASIS_POINTS_PER_PERCENT = 100;
    private static final int WHOLE = 100 * BASIS_POINTS_PER_PERCENT;

    public static final Percentage ZERO = new Percentage(0);

    public Percentage {
        if (basisPoints < 0 || basisPoints > WHOLE) {
            throw new IllegalArgumentException("A percentage must be from 0% to 100%, was " + basisPoints + " basis points");
        }
    }

    public static Percentage of(int percent) {
        return new Percentage(Math.multiplyExact(percent, BASIS_POINTS_PER_PERCENT));
    }

    public static Percentage ofBasisPoints(int basisPoints) {
        return new Percentage(basisPoints);
    }

    /** This share of {@code amount}, rounded half-up to the nearest sen. */
    public Money of(Money amount) {
        BigDecimal share = BigDecimal.valueOf(amount.sen())
                .multiply(BigDecimal.valueOf(basisPoints))
                .divide(BigDecimal.valueOf(WHOLE), 0, RoundingMode.HALF_UP);
        return Money.ofSen(share.longValueExact());
    }

    public boolean isGreaterThan(Percentage other) {
        return basisPoints > other.basisPoints;
    }

    @Override
    public int compareTo(Percentage other) {
        return Integer.compare(basisPoints, other.basisPoints);
    }

    /** Written as Homa shows it, for example {@code 15%} or {@code 12.5%}. */
    @Override
    public String toString() {
        return BigDecimal.valueOf(basisPoints, 2).stripTrailingZeros().toPlainString() + "%";
    }
}
