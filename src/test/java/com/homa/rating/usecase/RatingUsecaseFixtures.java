package com.homa.rating.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.CHECK_IN_PERIOD;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.PROTECTION_WINDOW;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.TOTAL_RESERVATION_FEE;
import static com.homa.fixtures.HomaFixtures.at;

import com.homa.ie.events.ReservationCancelledIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.kernel.Money;
import com.homa.rating.domain.CustomerId;
import com.homa.rating.domain.Party;
import com.homa.rating.domain.ProviderId;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.usecase.dto.SubmitRatingRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The §0 example as Rating's use cases see it. Aisyah's standard reservation is marked Completed on Saturday 5
 * December 14:00; its Protection Window ends on Sunday 6 December 14:00, so its Rating Window closes on Sunday 13
 * December 14:00 (RTG-2).
 */
public final class RatingUsecaseFixtures {

    private RatingUsecaseFixtures() {
    }

    public static final String RESERVATION_ID = "reservation-aisyah-standard";
    public static final String AISYAH_ID = AISYAH_CUSTOMER_ID;
    public static final String KEMAS_HOME_ID = KEMAS_HOME_PROVIDER_ID;
    public static final String DANIEL_ID = "customer-daniel";
    public static final String HANA_CLEANING_ID = "provider-hana-cleaning";
    public static final String SUPPORT_ID = "support-farah";

    public static final Party AISYAH = Party.customer(new CustomerId(AISYAH_ID));
    public static final Party KEMAS_HOME = Party.provider(new ProviderId(KEMAS_HOME_ID));
    public static final Party DANIEL = Party.customer(new CustomerId(DANIEL_ID));
    public static final Party HANA_CLEANING = Party.provider(new ProviderId(HANA_CLEANING_ID));

    public static final Instant COMPLETED_AT = JOB_END;
    public static final Instant PROTECTION_WINDOW_END = PROTECTION_WINDOW.end();
    /** Seven days after the Protection Window ends: Sunday 13 December 14:00. */
    public static final Instant WINDOW_CLOSES_AT = at(2026, 12, 13, 14, 0);
    public static final Instant AISYAH_RATES_AT = at(2026, 12, 5, 20, 0);
    public static final Instant KEMAS_HOME_RATES_AT = at(2026, 12, 7, 9, 0);
    /** Aisyah cancels on Friday 22:00, after Standard's cut-off: she pays the RM45 fee. */
    public static final Instant FRIDAY_22_00 = at(2026, 12, 4, 22, 0);

    public static final Money NOTHING = Money.myr(0);
    public static final Money BASE_CANCELLATION_FEE = Money.myr(45);

    public static ReservationCompletedIE completed(String eventId) {
        return completed(eventId, RESERVATION_ID, COMPLETED_AT);
    }

    public static ReservationCompletedIE completed(String eventId, String reservationId, Instant completedAt) {
        return new ReservationCompletedIE(eventId, reservationId, AISYAH_ID, KEMAS_HOME_ID, TOTAL_RESERVATION_FEE,
                PROTECTION_WINDOW_END, completedAt, REQUESTED_AT, List.of("photo-after"), completedAt);
    }

    /** What the Completed event opens for the standard reservation. */
    public static RatingSession openSession() {
        return openSession(RESERVATION_ID);
    }

    public static RatingSession openSession(String reservationId) {
        return RatingSession.open(new ReservationId(reservationId), new CustomerId(AISYAH_ID),
                new ProviderId(KEMAS_HOME_ID), COMPLETED_AT, PROTECTION_WINDOW_END, REQUESTED_AT);
    }

    public static ReservationCancelledIE cancelled(String eventId, CancelledAs cancelledAs) {
        return cancelled(eventId, RESERVATION_ID, cancelledAs);
    }

    /** A Cancelled event for a reservation, as the settlement worked out its fee and fine. */
    public static ReservationCancelledIE cancelled(String eventId, String reservationId, CancelledAs cancelledAs) {
        return new ReservationCancelledIE(eventId, reservationId, AISYAH_ID, KEMAS_HOME_ID, cancelledAs.cause(),
                FRIDAY_22_00, REQUESTED_AT, CHECK_IN_PERIOD.start(), PROTECTION_WINDOW_END, "settlement-" + eventId,
                cancelledAs.customerFee(), TOTAL_RESERVATION_FEE.minus(cancelledAs.customerFee()), NOTHING,
                cancelledAs.providerFine(), cancelledAs.customerFee(), FRIDAY_22_00);
    }

    /** The cause of a cancellation with the fee the customer pays and the fine the provider gets. */
    public record CancelledAs(String cause, Money customerFee, Money providerFine) {

        public static final CancelledAs CUSTOMER_CANCELLED_WITH_FEE =
                new CancelledAs("CUSTOMER_CANCELLED", BASE_CANCELLATION_FEE, NOTHING);
        public static final CancelledAs CUSTOMER_CANCELLED_FREE =
                new CancelledAs("CUSTOMER_CANCELLED", NOTHING, NOTHING);
        public static final CancelledAs CUSTOMER_LAST_MINUTE =
                new CancelledAs("CUSTOMER_LAST_MINUTE", Money.myr(65), NOTHING);
        public static final CancelledAs CUSTOMER_NO_SHOW =
                new CancelledAs("CUSTOMER_NO_SHOW", Money.myr("105.50"), NOTHING);
        public static final CancelledAs CUSTOMER_SUSPENDED = new CancelledAs("CUSTOMER_SUSPENDED", NOTHING, NOTHING);
        public static final CancelledAs PROVIDER_CANCELLED =
                new CancelledAs("PROVIDER_CANCELLED", NOTHING, BASE_CANCELLATION_FEE);
        public static final CancelledAs PROVIDER_LAST_MINUTE =
                new CancelledAs("PROVIDER_LAST_MINUTE", NOTHING, Money.myr(85));
        public static final CancelledAs NO_ROSTER = new CancelledAs("NO_ROSTER", NOTHING, Money.myr(69));
        public static final CancelledAs PROVIDER_NO_SHOW = new CancelledAs("PROVIDER_NO_SHOW", NOTHING, Money.myr(105));
        public static final CancelledAs PROVIDER_SUSPENDED_BY_SUPPORT =
                new CancelledAs("PROVIDER_SUSPENDED", NOTHING, BASE_CANCELLATION_FEE);
        public static final CancelledAs PROVIDER_SUSPENDED_BY_FLAG =
                new CancelledAs("PROVIDER_SUSPENDED", NOTHING, NOTHING);
    }

    /** A rating request by Aisyah: stars only. */
    public static SubmitRatingRequest aisyahRates(int stars) {
        return new SubmitRatingRequest(AISYAH_ID, RESERVATION_ID, stars, null, Map.of(), List.of());
    }

    /** A rating request by Kemas Home: stars only. */
    public static SubmitRatingRequest kemasHomeRates(int stars) {
        return new SubmitRatingRequest(KEMAS_HOME_ID, RESERVATION_ID, stars, null, Map.of(), List.of());
    }
}
