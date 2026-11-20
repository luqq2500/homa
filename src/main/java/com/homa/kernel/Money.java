package com.homa.kernel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An exact amount of money in one currency. Arithmetic never rounds; {@link #roundedToSen()} rounds half up to
 * two decimal places and is called only when a settlement line is produced (PAY-8). Two amounts are equal when
 * they have the same currency and the same value, whatever their scale (RM45 equals RM45.00).
 */
public final class Money implements Comparable<Money> {

    public static final Currency MYR = Currency.getInstance("MYR");
    private static final int SEN_SCALE = 2;

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount, "amount");
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /** Ringgit, from a decimal string such as "25" or "39.375". */
    public static Money myr(String amount) {
        return new Money(new BigDecimal(amount), MYR);
    }

    public static Money myr(long amount) {
        return new Money(BigDecimal.valueOf(amount), MYR);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public Money plus(Money other) {
        return new Money(amount.add(sameCurrency(other).amount), currency);
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(sameCurrency(other).amount), currency);
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(Objects.requireNonNull(factor, "factor")), currency);
    }

    public Money times(long factor) {
        return times(BigDecimal.valueOf(factor));
    }

    /** Rounds once to the nearest sen, half up (PAY-8): RM39.375 becomes RM39.38. */
    public Money roundedToSen() {
        return new Money(amount.setScale(SEN_SCALE, RoundingMode.HALF_UP), currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public Money min(Money other) {
        return compareTo(other) <= 0 ? this : other;
    }

    public Money max(Money other) {
        return compareTo(other) >= 0 ? this : other;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(sameCurrency(other).amount);
    }

    private Money sameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot combine " + currency + " with " + other.currency);
        }
        return other;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other
                && currency.equals(other.currency)
                && amount.compareTo(other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
}
