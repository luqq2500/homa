package com.homa.kernel;

import java.math.BigDecimal;

/**
 * An amount of Malaysian ringgit, held as whole sen. Never a floating-point number.
 *
 * <p>Money may be negative, so it can express a difference such as a price change. Arithmetic that
 * would overflow a {@code long} fails with {@link ArithmeticException} rather than wrapping.
 */
public record Money(long sen) implements Comparable<Money> {

    private static final int SEN_PER_RINGGIT = 100;

    public static final Money ZERO = new Money(0);

    public static Money ofSen(long sen) {
        return new Money(sen);
    }

    public static Money ofRinggit(long ringgit) {
        return new Money(Math.multiplyExact(ringgit, SEN_PER_RINGGIT));
    }

    /**
     * Reads an amount written in ringgit, such as {@code "230.00"}, {@code "31.5"} or {@code "-69"}.
     *
     * @throws IllegalArgumentException if the text is not a number or has more than two decimal places
     */
    public static Money parse(String ringgit) {
        try {
            return new Money(new BigDecimal(ringgit.trim()).movePointRight(2).longValueExact());
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Not an amount in ringgit and sen: " + ringgit, e);
        }
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(sen, other.sen));
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(sen, other.sen));
    }

    public Money times(long factor) {
        return new Money(Math.multiplyExact(sen, factor));
    }

    public Money min(Money other) {
        return isGreaterThan(other) ? other : this;
    }

    public boolean isZero() {
        return sen == 0;
    }

    public boolean isNegative() {
        return sen < 0;
    }

    public boolean isGreaterThan(Money other) {
        return sen > other.sen;
    }

    public boolean isLessThan(Money other) {
        return sen < other.sen;
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(sen, other.sen);
    }

    /** Written the way Homa shows money, for example {@code RM230.00} or {@code -RM69.00}. */
    @Override
    public String toString() {
        String sign = isNegative() ? "-" : "";
        return sign + "RM" + BigDecimal.valueOf(Math.abs(sen), 2).toPlainString();
    }
}
