package kernel;

/** A whole-number percentage from 0 to 100. */
public record Percentage(int percent) implements Comparable<Percentage> {

    public static final Percentage ZERO = new Percentage(0);

    public Percentage {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("A percentage is from 0 to 100: " + percent);
        }
    }

    public static Percentage of(int percent) {
        return new Percentage(percent);
    }

    /** This share of the amount, rounded half up to the nearest sen. */
    public Money of(Money amount) {
        return Money.ofSen((Math.multiplyExact(amount.sen(), percent) + 50) / 100);
    }

    /** This share of the amount, rounded down to the sen. */
    public Money roundedDownOf(Money amount) {
        return Money.ofSen(Math.multiplyExact(amount.sen(), percent) / 100);
    }

    public boolean isZero() {
        return percent == 0;
    }

    public boolean isMoreThan(Percentage other) {
        return percent > other.percent;
    }

    @Override
    public int compareTo(Percentage other) {
        return Integer.compare(percent, other.percent);
    }
}
