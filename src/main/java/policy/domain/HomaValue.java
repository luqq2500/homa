package policy.domain;

/** The values Homa sets for its policies, each with its unit and its setting at launch. */
public enum HomaValue {
    SERVICE_FEE(Unit.PERCENT, 15),
    MINIMUM_HOURS(Unit.HOURS, 1),
    MAXIMUM_HOURS(Unit.HOURS, 6),
    MINIMUM_NOTICE(Unit.MINUTES, 2 * 60),
    BOOKING_HORIZON(Unit.MINUTES, 30 * 24 * 60),
    TEAM_REMINDER_TIME(Unit.MINUTES, 6 * 60),
    EARLIEST_CHECK_IN(Unit.MINUTES, 15),
    CHECK_IN_PERIOD(Unit.MINUTES, 15),
    CHECK_IN_RADIUS(Unit.METRES, 300),
    CUSTOMER_NO_SHOW_WAIT(Unit.MINUTES, 15),
    CUSTOMER_NO_SHOW_FEE(Unit.PERCENT, 30),
    NO_SHOW_REPORT_WINDOW(Unit.MINUTES, 60),
    REQUEST_EXPIRY_TIME(Unit.MINUTES, 24 * 60),
    RESPONSE_DEADLINE(Unit.MINUTES, 60),
    COMPLETION_REMINDER_TIME(Unit.MINUTES, 30),
    COMPLETION_DEADLINE(Unit.MINUTES, 24 * 60),
    EVIDENCE_RETENTION_PERIOD(Unit.MINUTES, 120 * 24 * 60),
    DISPUTE_WINDOW_EXTENSION(Unit.MINUTES, 24 * 60),
    FEE_WINDOW(Unit.MINUTES, 24 * 60),
    STANDARD_CANCELLATION_FEE(Unit.PERCENT, 10),
    FIRM_CANCELLATION_FEE(Unit.PERCENT, 20),
    PROVIDER_LATE_CANCELLATION_FINE(Unit.PERCENT, 20),
    PROVIDER_NO_SHOW_FINE(Unit.PERCENT, 30),
    FINE_DEDUCTION_CAP(Unit.PERCENT, 50),
    RATING_WINDOW(Unit.MINUTES, 14 * 24 * 60),
    MINIMUM_RATINGS_TO_SHOW(Unit.COUNT, 3);

    private final Unit unit;
    private final long launchAmount;

    HomaValue(Unit unit, long launchAmount) {
        this.unit = unit;
        this.launchAmount = launchAmount;
    }

    public Unit unit() {
        return unit;
    }

    public long launchAmount() {
        return launchAmount;
    }
}
