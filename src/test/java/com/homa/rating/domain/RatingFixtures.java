package com.homa.rating.domain;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.PROTECTION_WINDOW;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.at;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The §0 example as Rating sees it: Aisyah rates Kemas Home after the standard reservation, marked Completed on
 * Saturday 5 December 14:00. The Protection Window ends on Sunday 6 December 14:00, so the Rating Window closes on
 * Sunday 13 December 14:00 (RTG-2).
 */
public final class RatingFixtures {

    private RatingFixtures() {
    }

    public static final ReservationId RESERVATION = new ReservationId("reservation-aisyah-standard");
    public static final CustomerId AISYAH = new CustomerId(AISYAH_CUSTOMER_ID);
    public static final ProviderId KEMAS_HOME = new ProviderId(KEMAS_HOME_PROVIDER_ID);
    public static final Party AISYAH_PARTY = Party.customer(AISYAH);
    public static final Party KEMAS_HOME_PARTY = Party.provider(KEMAS_HOME);

    public static final Instant COMPLETED_AT = JOB_END;
    public static final Instant PROTECTION_WINDOW_END = PROTECTION_WINDOW.end();
    /** Seven days after the Protection Window ends: Sunday 13 December 14:00. */
    public static final Instant WINDOW_CLOSES_AT = at(2026, 12, 13, 14, 0);
    /** Aisyah rates on Saturday evening; Kemas Home on Monday morning. */
    public static final Instant AISYAH_RATES_AT = at(2026, 12, 5, 20, 0);
    public static final Instant KEMAS_HOME_RATES_AT = at(2026, 12, 7, 9, 0);

    /** The session the Completed event opens for the standard reservation. */
    public static RatingSession openSession() {
        return RatingSession.open(RESERVATION, AISYAH, KEMAS_HOME, COMPLETED_AT, PROTECTION_WINDOW_END,
                REQUESTED_AT);
    }

    /** Aisyah's rating of Kemas Home: stars only. */
    public static Rating aisyahRates(int stars, Instant at) {
        return Rating.by(RatingId.generate(), AISYAH_PARTY, new Stars(stars), null, Map.of(), List.of(), at);
    }

    /** Kemas Home's rating of Aisyah: stars only. */
    public static Rating kemasHomeRates(int stars, Instant at) {
        return Rating.by(RatingId.generate(), KEMAS_HOME_PARTY, new Stars(stars), null, Map.of(), List.of(), at);
    }

    /** A session in which both have rated, so it is revealed. */
    public static RatingSession revealedSession(Rating aisyahsRating, Rating kemasHomesRating) {
        RatingSession session = openSession();
        session.submit(aisyahsRating);
        session.submit(kemasHomesRating);
        return session;
    }
}
