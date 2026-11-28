package com.homa.rating.usecase;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.BASE_CANCELLATION_FEE;
import static com.homa.rating.usecase.RatingUsecaseFixtures.DANIEL_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.FRIDAY_22_00;
import static com.homa.rating.usecase.RatingUsecaseFixtures.HANA_CLEANING_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.NOTHING;
import static com.homa.rating.usecase.RatingUsecaseFixtures.openSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.rating.domain.Cancellation;
import com.homa.rating.domain.CancellationCause;
import com.homa.rating.domain.Rating;
import com.homa.rating.domain.RatingCategory;
import com.homa.rating.domain.RatingId;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.ReliabilityRecord;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.domain.Stars;
import com.homa.rating.usecase.dto.ViewRatingsAndReliabilityRequest;
import com.homa.rating.usecase.dto.ViewRatingsAndReliabilityResponse;
import com.homa.rating.usecase.exception.NotAllowedException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ViewRatingsAndReliabilityTest {

    private static final Instant TWELVE_MONTHS_AFTER_FRIDAY = at(2027, 12, 4, 22, 0);

    private final InMemoryRatingSessionRepository sessions = new InMemoryRatingSessionRepository();
    private final InMemoryReliabilityRecordRepository reliability = new InMemoryReliabilityRecordRepository();
    private final FixedTimeSource time = new FixedTimeSource(at(2026, 12, 20, 10, 0));
    private final ViewRatingsAndReliability view = new ViewRatingsAndReliability(sessions, reliability, time);

    private void aisyahAndKemasHomeHaveRated() {
        RatingSession session = openSession();
        session.submit(Rating.by(RatingId.generate(), AISYAH, new Stars(4), null,
                Map.of(RatingCategory.QUALITY_OF_WORK, new Stars(5), RatingCategory.PUNCTUALITY, new Stars(4)),
                List.of(), AISYAH_RATES_AT));
        session.submit(Rating.by(RatingId.generate(), KEMAS_HOME, new Stars(3), null,
                Map.of(RatingCategory.ACCESS_AND_READINESS, new Stars(3)), List.of(), KEMAS_HOME_RATES_AT));
        sessions.given(session);
    }

    private static ViewRatingsAndReliabilityRequest customerViewsKemasHome(String viewerId) {
        return new ViewRatingsAndReliabilityRequest(viewerId, "CUSTOMER", KEMAS_HOME_ID, "PROVIDER");
    }

    private void kemasHomeCancelledOnce() {
        ReliabilityRecord record = ReliabilityRecord.openFor(KEMAS_HOME);
        record.recordCancellation(new Cancellation(new ReservationId("reservation-1"),
                CancellationCause.PROVIDER_CANCELLED, NOTHING, BASE_CANCELLATION_FEE, FRIDAY_22_00));
        reliability.given(record);
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("Other customers see Aisyah's 4 stars on Kemas Home's page, with the averages of her three "
            + "categories.")
    void aCustomerSeesAProvidersRevealedRatings() {
        aisyahAndKemasHomeHaveRated();

        ViewRatingsAndReliabilityResponse response = view.view(customerViewsKemasHome(DANIEL_ID));

        assertThat(response.subjectId()).isEqualTo(KEMAS_HOME_ID);
        assertThat(response.subjectRole()).isEqualTo("PROVIDER");
        assertThat(response.ratingCount()).isEqualTo(1);
        assertThat(response.averageStars()).isEqualTo(new BigDecimal("4.0"));
        assertThat(response.categoryAverages()).containsExactlyInAnyOrderEntriesOf(
                Map.of("QUALITY_OF_WORK", new BigDecimal("5.0"), "PUNCTUALITY", new BigDecimal("4.0")));
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("Kemas Home sees its own ratings, and Aisyah's customer ratings are seen by providers, with the "
            + "provider's categories.")
    void aPartySeesItsOwnAndTheOtherSidesRatings() {
        aisyahAndKemasHomeHaveRated();

        ViewRatingsAndReliabilityResponse own = view.view(
                new ViewRatingsAndReliabilityRequest(KEMAS_HOME_ID, "PROVIDER", KEMAS_HOME_ID, "PROVIDER"));
        ViewRatingsAndReliabilityResponse aisyah = view.view(
                new ViewRatingsAndReliabilityRequest(HANA_CLEANING_ID, "PROVIDER", AISYAH_ID, "CUSTOMER"));
        ViewRatingsAndReliabilityResponse herself = view.view(
                new ViewRatingsAndReliabilityRequest(AISYAH_ID, "CUSTOMER", AISYAH_ID, "CUSTOMER"));

        assertThat(own.ratingCount()).isEqualTo(1);
        assertThat(aisyah.subjectRole()).isEqualTo("CUSTOMER");
        assertThat(aisyah.ratingCount()).isEqualTo(1);
        assertThat(aisyah.averageStars()).isEqualTo(new BigDecimal("3.0"));
        assertThat(aisyah.categoryAverages()).isEqualTo(Map.of("ACCESS_AND_READINESS", new BigDecimal("3.0")));
        assertThat(herself.averageStars()).isEqualTo(new BigDecimal("3.0"));
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("Hana Cleaning, another provider, can't see Kemas Home's ratings; nor can Daniel see Aisyah's.")
    void aPartyOfTheSameSideIsRefused() {
        aisyahAndKemasHomeHaveRated();

        assertThatThrownBy(() -> view.view(new ViewRatingsAndReliabilityRequest(HANA_CLEANING_ID, "PROVIDER",
                KEMAS_HOME_ID, "PROVIDER"))).isInstanceOf(NotAllowedException.class)
                .hasMessageContaining(HANA_CLEANING_ID).hasMessageContaining(KEMAS_HOME_ID);
        assertThatThrownBy(() -> view.view(new ViewRatingsAndReliabilityRequest(DANIEL_ID, "CUSTOMER", AISYAH_ID,
                "CUSTOMER"))).isInstanceOf(NotAllowedException.class);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("Ratings still hidden by the double blind aren't on the page: Aisyah rated, Kemas Home hasn't.")
    void hiddenRatingsAreNotShown() {
        RatingSession session = openSession();
        session.submit(Rating.by(RatingId.generate(), AISYAH, new Stars(5), null, Map.of(), List.of(), AISYAH_RATES_AT));
        sessions.given(session);

        ViewRatingsAndReliabilityResponse response = view.view(customerViewsKemasHome(DANIEL_ID));

        assertThat(response.ratingCount()).isZero();
        assertThat(response.averageStars()).isNull();
        assertThat(response.categoryAverages()).isEmpty();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Kemas Home shows \"Cancellations: 1, No-rosters: 0, No-shows: 0\" to customers.")
    void aProvidersReliabilityIsShownToCustomers() {
        kemasHomeCancelledOnce();

        ViewRatingsAndReliabilityResponse response = view.view(customerViewsKemasHome(AISYAH_ID));

        assertThat(response.cancellations()).isEqualTo(1);
        assertThat(response.noRosters()).isZero();
        assertThat(response.noShows()).isZero();
        assertThat(response.membersLeft()).isZero();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Providers see Aisyah's RM45 cancellation; her free one isn't counted, as it was never logged.")
    void aCustomersReliabilityIsShownToProviders() {
        ReliabilityRecord record = ReliabilityRecord.openFor(AISYAH);
        record.recordCancellation(new Cancellation(new ReservationId("reservation-free"),
                CancellationCause.CUSTOMER_CANCELLED, NOTHING, NOTHING, FRIDAY_22_00));
        record.recordCancellation(new Cancellation(new ReservationId("reservation-fee"),
                CancellationCause.CUSTOMER_CANCELLED, BASE_CANCELLATION_FEE, NOTHING, FRIDAY_22_00));
        record.recordCancellation(new Cancellation(new ReservationId("reservation-no-show"),
                CancellationCause.CUSTOMER_NO_SHOW, Money.myr("105.50"), NOTHING, FRIDAY_22_00));
        reliability.given(record);

        ViewRatingsAndReliabilityResponse response = view.view(
                new ViewRatingsAndReliabilityRequest(KEMAS_HOME_ID, "PROVIDER", AISYAH_ID, "CUSTOMER"));

        assertThat(response.cancellations()).isEqualTo(1);
        assertThat(response.noShows()).isEqualTo(1);
        assertThat(response.noRosters()).isZero();
        assertThat(response.membersLeft()).isZero();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Counts cover the last 12 months: one minute before 4 December the next year Kemas Home's "
            + "cancellation still shows; at that moment it doesn't.")
    void reliabilityCountsOnlyTheLastTwelveMonths() {
        kemasHomeCancelledOnce();

        time.set(TWELVE_MONTHS_AFTER_FRIDAY.minus(Duration.ofMinutes(1)));
        ViewRatingsAndReliabilityResponse justInside = view.view(customerViewsKemasHome(AISYAH_ID));
        time.set(TWELVE_MONTHS_AFTER_FRIDAY);
        ViewRatingsAndReliabilityResponse exactlyTwelveMonths = view.view(customerViewsKemasHome(AISYAH_ID));

        assertThat(justInside.cancellations()).isEqualTo(1);
        assertThat(exactlyTwelveMonths.cancellations()).isZero();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A provider nobody has logged anything about shows 0 everywhere.")
    void aPartyWithNoRecordShowsZeroes() {
        ViewRatingsAndReliabilityResponse response = view.view(customerViewsKemasHome(AISYAH_ID));

        assertThat(response.cancellations()).isZero();
        assertThat(response.noRosters()).isZero();
        assertThat(response.noShows()).isZero();
        assertThat(response.membersLeft()).isZero();
        assertThat(response.ratingCount()).isZero();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Hana Cleaning can't see Kemas Home's reliability either: it is another provider.")
    void anotherProviderDoesntSeeReliability() {
        kemasHomeCancelledOnce();

        assertThatThrownBy(() -> view.view(new ViewRatingsAndReliabilityRequest(HANA_CLEANING_ID, "PROVIDER",
                KEMAS_HOME_ID, "PROVIDER"))).isInstanceOf(NotAllowedException.class);
    }
}
