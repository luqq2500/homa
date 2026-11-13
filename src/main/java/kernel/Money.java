package kernel;

/** An amount of Malaysian ringgit, held in sen. Never negative. */
public record Money(long sen) implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    public Money {
        if (sen < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + sen + " sen");
        }
    }

    public static Money ofSen(long sen) {
        return new Money(sen);
    }

    public static Money ofRinggit(long ringgit) {
        return new Money(Math.multiplyExact(ringgit, 100));
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(sen, other.sen));
    }

    /** Takes {@code other} away; taking away more than there is fails, because money is never negative. */
    public Money minus(Money other) {
        return new Money(sen - other.sen);
    }

    public Money times(long multiplier) {
        return new Money(Math.multiplyExact(sen, multiplier));
    }

    public boolean isZero() {
        return sen == 0;
    }

    public boolean isMoreThan(Money other) {
        return sen > other.sen;
    }

    public Money min(Money other) {
        return isMoreThan(other) ? other : this;
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(sen, other.sen);
    }
}
