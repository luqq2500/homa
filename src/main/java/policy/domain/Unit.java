package policy.domain;

/** What a Homa-set value is measured in, and the amounts that make sense for it. */
public enum Unit {
    PERCENT(0, 100),
    MINUTES(1, Long.MAX_VALUE),
    HOURS(1, Long.MAX_VALUE),
    METRES(1, Long.MAX_VALUE),
    COUNT(1, Long.MAX_VALUE);

    private final long lowest;
    private final long highest;

    Unit(long lowest, long highest) {
        this.lowest = lowest;
        this.highest = highest;
    }

    public boolean accepts(long amount) {
        return amount >= lowest && amount <= highest;
    }
}
