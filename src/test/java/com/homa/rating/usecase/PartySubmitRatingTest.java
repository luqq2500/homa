package com.homa.rating.usecase;

import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.DANIEL_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.HANA_CLEANING_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.RESERVATION_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.WINDOW_CLOSES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.aisyahRates;
import static com.homa.rating.usecase.RatingUsecaseFixtures.kemasHomeRates;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ProviderRatingRevealedIE;
import com.homa.rating.domain.PartyRole;
import com.homa.rating.domain.Rating;
import com.homa.rating.domain.RatingCategory;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.domain.Stars;
import com.homa.rating.domain.exception.InvalidStarsException;
import com.homa.rating.domain.exception.MediaNotAllowedException;
import com.homa.rating.domain.exception.RatingAlreadySubmittedException;
import com.homa.rating.domain.exception.RatingCategoryNotForAuthorException;
import com.homa.rating.domain.exception.RatingWindowClosedException;
import com.homa.rating.usecase.dto.SubmitRatingRequest;
import com.homa.rating.usecase.dto.SubmitRatingResponse;
import com.homa.rating.usecase.exception.NotAllowedException;
import com.homa.rating.usecase.exception.RatingSessionChangedException;
import com.homa.rating.usecase.exception.RatingSessionNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PartySubmitRatingTest {

    private final InMemoryRatingSessionRepository sessions = new InMemoryRatingSessionRepository();
    private final RecordingRatingIEPublisher publisher = new RecordingRatingIEPublisher();
    private final FixedTimeSource time = new FixedTimeSource(AISYAH_RATES_AT);
    private final RatingEvents events = new RatingEvents(new SequentialEventIds(), time);
    private final PartySubmitRating submit = new PartySubmitRating(sessions,
            new ProviderRatingUpdates(sessions, publisher, events), time);

    private RatingSession openSession() {
        RatingSession session = RatingUsecaseFixtures.openSession();
        sessions.given(session);
        return session;
    }

    @Test
    @Tag("RTG-1")
    @Tag("RTG-3")
    @DisplayName("Aisyah rates Kemas Home on Saturday evening: her rating is taken and stays hidden, and nothing is "
            + "published.")
    void aisyahRatesKemasHomeAndItStaysHiddenUntilTheOtherSideRates() {
        RatingSession session = openSession();

        SubmitRatingResponse response = submit.submit(aisyahRates(4));

        assertThat(response.reservationId()).isEqualTo(RESERVATION_ID);
        assertThat(response.authorRole()).isEqualTo("CUSTOMER");
        assertThat(response.revealed()).isFalse();
        assertThat(response.revealedAt()).isNull();
        Rating stored = session.ratingBy(PartyRole.CUSTOMER).orElseThrow();
        assertThat(response.ratingId()).isEqualTo(stored.id().value());
        assertThat(stored.stars()).isEqualTo(new Stars(4));
        assertThat(stored.submittedAt()).isEqualTo(AISYAH_RATES_AT);
        assertThat(sessions.saves()).containsExactly(session);
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("RTG-1")
    @Tag("RTG-3")
    @DisplayName("Aisyah rates on Saturday and Kemas Home on Monday: both show on Monday, and Listing is told what "
            + "customers say about Kemas Home.")
    void bothRatingsAreRevealedWhenTheSecondComesInAndListingIsTold() {
        openSession();
        submit.submit(new SubmitRatingRequest(AISYAH_ID, RESERVATION_ID, 4, "Spotless kitchen",
                Map.of("QUALITY_OF_WORK", 5, "PUNCTUALITY", 4, "PROFESSIONALISM", 5), List.of()));
        time.set(KEMAS_HOME_RATES_AT);

        SubmitRatingResponse response = submit.submit(kemasHomeRates(5));

        assertThat(response.authorRole()).isEqualTo("PROVIDER");
        assertThat(response.revealed()).isTrue();
        assertThat(response.revealedAt()).isEqualTo(KEMAS_HOME_RATES_AT);
        assertThat(publisher.published()).containsExactly(new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 1,
                "4.0", "5.0", "4.0", "5.0", KEMAS_HOME_RATES_AT));
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("When Kemas Home rates first and Aisyah second, her rating completes the pair and the page "
            + "numbers are published.")
    void theProviderCanRateFirst() {
        openSession();
        submit.submit(kemasHomeRates(5));
        assertThat(publisher.published()).isEmpty();
        time.set(KEMAS_HOME_RATES_AT);

        SubmitRatingResponse response = submit.submit(aisyahRates(3));

        assertThat(response.revealed()).isTrue();
        assertThat(publisher.published()).containsExactly(new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 1,
                "3.0", null, null, null, KEMAS_HOME_RATES_AT));
    }

    @Test
    @Tag("RTG-4")
    @DisplayName("A rating has 1 to 5 stars: Aisyah can't give 0 or 6, and nothing is saved.")
    void starsRunFromOneToFive() {
        openSession();

        assertThatThrownBy(() -> submit.submit(aisyahRates(0))).isInstanceOf(InvalidStarsException.class);
        assertThatThrownBy(() -> submit.submit(aisyahRates(6))).isInstanceOf(InvalidStarsException.class);
        submit.submit(aisyahRates(1));

        assertThat(sessions.saves()).hasSize(1);
    }

    @Test
    @Tag("RTG-4")
    @DisplayName("Aisyah gives 4 stars with no text and no category stars: that is enough.")
    void aDescriptionAndCategoryStarsAreOptional() {
        RatingSession session = openSession();

        submit.submit(aisyahRates(4));

        Rating stored = session.ratingBy(PartyRole.CUSTOMER).orElseThrow();
        assertThat(stored.description()).isEmpty();
        assertThat(stored.categoryStars()).isEmpty();
        assertThat(stored.mediaRefs()).isEmpty();
    }

    @Test
    @Tag("RTG-5")
    @DisplayName("Aisyah adds a photo of her kitchen to her rating.")
    void aCustomersRatingCanIncludeMedia() {
        RatingSession session = openSession();

        submit.submit(new SubmitRatingRequest(AISYAH_ID, RESERVATION_ID, 4, null, Map.of(), List.of("photo-kitchen")));

        assertThat(session.ratingBy(PartyRole.CUSTOMER).orElseThrow().mediaRefs()).containsExactly("photo-kitchen");
    }

    @Test
    @Tag("RTG-5")
    @DisplayName("Kemas Home can't add photos of her home: a provider's rating is text only, and nothing is saved.")
    void aProvidersRatingIsTextOnly() {
        RatingSession session = openSession();

        assertThatThrownBy(() -> submit.submit(new SubmitRatingRequest(KEMAS_HOME_ID, RESERVATION_ID, 4, "Lovely",
                Map.of(), List.of("photo-kitchen")))).isInstanceOf(MediaNotAllowedException.class);

        assertThat(sessions.saves()).isEmpty();
        assertThat(session.ratings()).isEmpty();
    }

    @Test
    @Tag("RTG-7")
    @DisplayName("Aisyah gives Quality of work 5, Punctuality 4 and Professionalism 5.")
    void customersRateProvidersOnTheirThreeCategories() {
        RatingSession session = openSession();

        submit.submit(new SubmitRatingRequest(AISYAH_ID, RESERVATION_ID, 5, null,
                Map.of("QUALITY_OF_WORK", 5, "PUNCTUALITY", 4, "PROFESSIONALISM", 5), List.of()));

        assertThat(session.ratingBy(PartyRole.CUSTOMER).orElseThrow().categoryStars()).containsExactlyInAnyOrderEntriesOf(
                Map.of(RatingCategory.QUALITY_OF_WORK, new Stars(5), RatingCategory.PUNCTUALITY, new Stars(4),
                        RatingCategory.PROFESSIONALISM, new Stars(5)));
    }

    @Test
    @Tag("RTG-7")
    @DisplayName("Aisyah can't rate Access and readiness, which is a provider's category; nor an unknown category.")
    void customersCantUseTheProvidersCategories() {
        openSession();

        assertThatThrownBy(() -> submit.submit(new SubmitRatingRequest(AISYAH_ID, RESERVATION_ID, 4, null,
                Map.of("ACCESS_AND_READINESS", 3), List.of()))).isInstanceOf(RatingCategoryNotForAuthorException.class);
        assertThatThrownBy(() -> submit.submit(new SubmitRatingRequest(AISYAH_ID, RESERVATION_ID, 4, null,
                Map.of("CLEANLINESS", 3), List.of()))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> submit.submit(new SubmitRatingRequest(AISYAH_ID, RESERVATION_ID, 4, null,
                Map.of("PUNCTUALITY", 9), List.of()))).isInstanceOf(InvalidStarsException.class);
        assertThat(sessions.saves()).isEmpty();
    }

    @Test
    @Tag("RTG-8")
    @DisplayName("Kemas Home gives Access and readiness 3, because the guardhouse code was missing.")
    void providersRateCustomersOnTheirThreeCategories() {
        RatingSession session = openSession();

        submit.submit(new SubmitRatingRequest(KEMAS_HOME_ID, RESERVATION_ID, 4, "The guardhouse code was missing.",
                Map.of("ACCURATE_REQUEST", 5, "ACCESS_AND_READINESS", 3, "RESPECT", 5), List.of()));

        Rating stored = session.ratingBy(PartyRole.PROVIDER).orElseThrow();
        assertThat(stored.categoryStars().get(RatingCategory.ACCESS_AND_READINESS)).isEqualTo(new Stars(3));
        assertThat(stored.description()).contains("The guardhouse code was missing.");
    }

    @Test
    @Tag("RTG-8")
    @DisplayName("Kemas Home can't rate Punctuality, which is a customer's category.")
    void providersCantUseTheCustomersCategories() {
        openSession();

        assertThatThrownBy(() -> submit.submit(new SubmitRatingRequest(KEMAS_HOME_ID, RESERVATION_ID, 4, null,
                Map.of("PUNCTUALITY", 4), List.of()))).isInstanceOf(RatingCategoryNotForAuthorException.class);
    }

    @Test
    @Tag("RTG-1")
    @DisplayName("Aisyah rates once: a second rating from her is refused and the first stands.")
    void eachSideRatesOnce() {
        RatingSession session = openSession();
        submit.submit(aisyahRates(4));

        assertThatThrownBy(() -> submit.submit(aisyahRates(1))).isInstanceOf(RatingAlreadySubmittedException.class);

        assertThat(sessions.saves()).hasSize(1);
        assertThat(session.ratingBy(PartyRole.CUSTOMER).orElseThrow().stars()).isEqualTo(new Stars(4));
    }

    @Test
    @Tag("RTG-1")
    @DisplayName("Daniel and Hana Cleaning have no part in Aisyah's reservation, so neither can rate it.")
    void onlyTheReservationsOwnPartiesRate() {
        openSession();

        assertThatThrownBy(() -> submit.submit(new SubmitRatingRequest(DANIEL_ID, RESERVATION_ID, 1, null, Map.of(),
                List.of()))).isInstanceOf(NotAllowedException.class).hasMessageContaining(DANIEL_ID);
        assertThatThrownBy(() -> submit.submit(new SubmitRatingRequest(HANA_CLEANING_ID, RESERVATION_ID, 1, null,
                Map.of(), List.of()))).isInstanceOf(NotAllowedException.class);
        assertThat(sessions.saves()).isEmpty();
    }

    @Test
    @Tag("RTG-9")
    @DisplayName("A Customer No Show gets no ratings: with no Completed job there is nothing to rate.")
    void onlyACompletedReservationGetsRatings() {
        assertThatThrownBy(() -> submit.submit(aisyahRates(4))).isInstanceOf(RatingSessionNotFoundException.class)
                .hasMessageContaining("RTG-9").hasMessageContaining(RESERVATION_ID);
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("One minute before the window closes on Sunday 13 December 14:00 Aisyah can still rate.")
    void aRatingOneMinuteBeforeTheWindowClosesIsTaken() {
        RatingSession session = openSession();
        time.set(WINDOW_CLOSES_AT.minus(Duration.ofMinutes(1)));

        submit.submit(aisyahRates(4));

        assertThat(session.ratingBy(PartyRole.CUSTOMER)).isPresent();
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("One minute after the window closes a rating is too late, and nothing is saved.")
    void aRatingOneMinuteAfterTheWindowClosesIsRefused() {
        RatingSession session = openSession();
        time.set(WINDOW_CLOSES_AT.plus(Duration.ofMinutes(1)));

        assertThatThrownBy(() -> submit.submit(aisyahRates(4))).isInstanceOf(RatingWindowClosedException.class);

        assertThat(sessions.saves()).isEmpty();
        assertThat(session.ratings()).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("When someone else saved the ratings first, the rating isn't saved and the caller asks again.")
    void aConflictingSaveIsLeftToTheCaller() {
        openSession();
        sessions.conflictOnNextSave(new ReservationId(RESERVATION_ID));

        assertThatThrownBy(() -> submit.submit(aisyahRates(4))).isInstanceOf(RatingSessionChangedException.class);

        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("A rating that completes the pair is stamped with the time it came in.")
    void theRevealMomentIsTheSecondRatingsTime() {
        openSession();
        submit.submit(aisyahRates(4));
        Instant monday = KEMAS_HOME_RATES_AT;
        time.set(monday);

        submit.submit(kemasHomeRates(2));

        assertThat(sessions.findByReservationId(new ReservationId(RESERVATION_ID)).orElseThrow().revealedAt())
                .contains(monday);
    }
}
