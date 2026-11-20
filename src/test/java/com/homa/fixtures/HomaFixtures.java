package com.homa.fixtures;

import com.homa.kernel.Money;
import com.homa.kernel.Percentage;
import com.homa.kernel.TimeInterval;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * The example setup (§0 of docs/homa-business-rule.md) that every rule case uses: Aisyah, Kemas Home Sdn Bhd and
 * the standard RM200 reservation. Holds only plain values and kernel types, so every context can build its own
 * objects from the same numbers in its own test fixtures (for example {@code ReservationFixtures}) without this
 * class depending on any context.
 *
 * <p>Dates: the reservation is made on Monday 30 November 2026 at 20:00 for Saturday 5 December 2026,
 * 10:00–14:00, all in Asia/Kuala_Lumpur.
 */
public final class HomaFixtures {

    private HomaFixtures() {
    }

    public static final ZoneId KUALA_LUMPUR = ZoneId.of("Asia/Kuala_Lumpur");

    // People and businesses
    public static final String AISYAH_CUSTOMER_ID = "customer-aisyah";
    public static final String AISYAH_NAME = "Aisyah";
    public static final String AISYAH_CITY = "SALAK_TINGGI";
    public static final String KEMAS_HOME_PROVIDER_ID = "provider-kemas-home";
    public static final String KEMAS_HOME_NAME = "Kemas Home Sdn Bhd";
    public static final String KEMAS_HOME_REGISTRATION_NO = "202301012345";
    public static final List<String> KEMAS_HOME_CITIES = List.of("SALAK_TINGGI", "CYBERJAYA", "PUTRAJAYA");

    // Team: helpers Siti and Nurul (citizens), helper Rina (foreign), supervisor Farid (citizen)
    public static final String SITI_MEMBER_ID = "member-siti";
    public static final String NURUL_MEMBER_ID = "member-nurul";
    public static final String RINA_MEMBER_ID = "member-rina";
    public static final String FARID_MEMBER_ID = "member-farid";

    // Kemas Home's listing
    public static final Money HOURLY_RATE_PER_HELPER = Money.myr(25);
    public static final Money FLAT_TRAVEL_FEE = Money.myr(20);
    public static final Percentage DISCOUNT_RATE = Percentage.of(10);
    public static final Duration DISCOUNT_MIN_HOURS = Duration.ofHours(3);
    public static final int DISCOUNT_MIN_HELPERS = 2;
    public static final Duration MINIMUM_NOTICE = Duration.ofHours(2);
    public static final String ACCEPTANCE_POLICY = "REQUEST";
    public static final String CANCELLATION_TIER = "STANDARD";
    public static final Duration JOB_BUFFER = Duration.ofMinutes(30);

    // The standard reservation
    public static final int STANDARD_HELPERS = 2;
    public static final Duration STANDARD_HOURS = Duration.ofHours(4);
    public static final Instant REQUESTED_AT = at(2026, 11, 30, 20, 0);
    public static final Instant JOB_START = at(2026, 12, 5, 10, 0);
    public static final Instant JOB_END = at(2026, 12, 5, 14, 0);
    public static final TimeInterval JOB_TIME = new TimeInterval(JOB_START, JOB_END);
    public static final TimeInterval CHECK_IN_PERIOD = new TimeInterval(at(2026, 12, 5, 9, 45), JOB_START);
    public static final Instant ACCEPTANCE_DEADLINE = at(2026, 12, 1, 20, 0);
    public static final TimeInterval PROTECTION_WINDOW = new TimeInterval(at(2026, 12, 5, 9, 45), at(2026, 12, 6, 14, 0));

    // The standard reservation's numbers
    public static final Money HOURS_FEE = Money.myr(180);
    public static final Money TOTAL_RESERVATION_FEE = Money.myr(200);
    public static final Money BASE_CANCELLATION_FEE = Money.myr(45);
    public static final Money DISCOUNTED_HOURLY_COST_ALL_HELPERS = Money.myr(45);
    public static final Percentage PLATFORM_FEE_RATE = Percentage.of("15.5");

    /** A wall-clock time in Kuala Lumpur. */
    public static Instant at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(KUALA_LUMPUR).toInstant();
    }

    /** A clock fixed at a Kuala Lumpur wall-clock time. */
    public static Clock clockAt(int year, int month, int day, int hour, int minute) {
        return Clock.fixed(at(year, month, day, hour, minute), KUALA_LUMPUR);
    }

    /** The clock at the moment the standard reservation is requested: Monday 20:00. */
    public static Clock requestedClock() {
        return Clock.fixed(REQUESTED_AT, KUALA_LUMPUR);
    }
}
