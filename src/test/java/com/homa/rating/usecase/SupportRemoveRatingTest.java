package com.homa.rating.usecase;

import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.RESERVATION_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.SUPPORT_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.openSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ProviderRatingRevealedIE;
import com.homa.rating.domain.PartyRole;
import com.homa.rating.domain.Rating;
import com.homa.rating.domain.RatingCategory;
import com.homa.rating.domain.RatingId;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.Stars;
import com.homa.rating.domain.exception.RatingNotFoundException;
import com.homa.rating.usecase.dto.RemoveRatingRequest;
import com.homa.rating.usecase.dto.RemoveRatingResponse;
import com.homa.rating.usecase.exception.NotAllowedException;
import com.homa.rating.usecase.exception.RatingSessionNotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class SupportRemoveRatingTest {

    private final InMemoryRatingSessionRepository sessions = new InMemoryRatingSessionRepository();
    private final RecordingRatingIEPublisher publisher = new RecordingRatingIEPublisher();
    private final FixedTimeSource time = new FixedTimeSource(com.homa.fixtures.HomaFixtures.at(2026, 12, 8, 10, 0));
    private final RatingEvents events = new RatingEvents(new SequentialEventIds(), time);
    private final SupportRemoveRating remove =
            new SupportRemoveRating(sessions, new ProviderRatingUpdates(sessions, publisher, events));

    private final Rating aisyahs = Rating.by(RatingId.generate(), AISYAH, new Stars(1), "Call me on 012-345 6789",
            Map.of(RatingCategory.PUNCTUALITY, new Stars(2)), List.of(), AISYAH_RATES_AT);
    private final Rating kemasHomes = Rating.by(RatingId.generate(), KEMAS_HOME, new Stars(5), null, Map.of(),
            List.of(), KEMAS_HOME_RATES_AT);

    private RatingSession revealedSession() {
        RatingSession session = openSession();
        session.submit(aisyahs);
        session.submit(kemasHomes);
        sessions.given(session);
        return session;
    }

    private RemoveRatingRequest supportRemoves(Rating rating) {
        return new RemoveRatingRequest(SUPPORT_ID, "SUPPORT", RESERVATION_ID, rating.id().value());
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Support removes a rating that includes a customer's phone number: Aisyah's rating is gone, Kemas "
            + "Home's of her stays, and Listing is told Kemas Home has no ratings left.")
    void supportRemovesARevealedRatingAndListingIsTold() {
        RatingSession session = revealedSession();

        RemoveRatingResponse response = remove.remove(supportRemoves(aisyahs));

        assertThat(response).isEqualTo(new RemoveRatingResponse(RESERVATION_ID, aisyahs.id().value(), "CUSTOMER"));
        assertThat(session.ratings()).containsExactly(kemasHomes);
        assertThat(session.showsRatingOfProvider()).isFalse();
        assertThat(sessions.saves()).containsExactly(session);
        assertThat(publisher.published()).containsExactly(new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 0,
                null, null, null, null, time.now()));
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("When Aisyah's rating is removed, the other ratings Kemas Home has stay in its averages.")
    void theEventKeepsTheOtherRatingsOfTheProvider() {
        RatingSession other = openSession("reservation-earlier");
        other.submit(Rating.by(RatingId.generate(), AISYAH, new Stars(4), null,
                Map.of(RatingCategory.PUNCTUALITY, new Stars(4)), List.of(), AISYAH_RATES_AT));
        other.submit(Rating.by(RatingId.generate(), KEMAS_HOME, new Stars(5), null, Map.of(), List.of(),
                KEMAS_HOME_RATES_AT));
        sessions.given(other);
        revealedSession();

        remove.remove(supportRemoves(aisyahs));

        assertThat(publisher.published()).containsExactly(new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 1,
                "4.0", null, "4.0", null, time.now()));
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Support removes Kemas Home's rating of Aisyah: nothing about Kemas Home's page changes, so "
            + "nothing is published.")
    void removingTheProvidersRatingPublishesNothing() {
        RatingSession session = revealedSession();

        RemoveRatingResponse response = remove.remove(supportRemoves(kemasHomes));

        assertThat(response.authorRole()).isEqualTo("PROVIDER");
        assertThat(session.ratings()).containsExactly(aisyahs);
        assertThat(sessions.saves()).containsExactly(session);
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Support removes a rating that is still hidden: nothing was shown, so nothing is published.")
    void removingAHiddenRatingPublishesNothing() {
        RatingSession session = openSession();
        session.submit(aisyahs);
        sessions.given(session);

        remove.remove(supportRemoves(aisyahs));

        assertThat(session.ratingBy(PartyRole.CUSTOMER)).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Only Support removes a rating: Aisyah, Kemas Home and a missing role are refused.")
    void onlySupportRemovesARating() {
        RatingSession session = revealedSession();

        assertThatThrownBy(() -> remove.remove(new RemoveRatingRequest(AISYAH_ID, "CUSTOMER", RESERVATION_ID,
                aisyahs.id().value()))).isInstanceOf(NotAllowedException.class).hasMessageContaining(AISYAH_ID);
        assertThatThrownBy(() -> remove.remove(new RemoveRatingRequest(KEMAS_HOME_ID, "PROVIDER", RESERVATION_ID,
                aisyahs.id().value()))).isInstanceOf(NotAllowedException.class);
        assertThatThrownBy(() -> remove.remove(new RemoveRatingRequest(SUPPORT_ID, null, RESERVATION_ID,
                aisyahs.id().value()))).isInstanceOf(NotAllowedException.class);

        assertThat(session.ratings()).hasSize(2);
        assertThat(sessions.saves()).isEmpty();
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Support can't remove a rating of a reservation that has no ratings, or one the reservation "
            + "doesn't have.")
    void aMissingSessionOrRatingIsRefused() {
        assertThatThrownBy(() -> remove.remove(supportRemoves(aisyahs)))
                .isInstanceOf(RatingSessionNotFoundException.class);

        revealedSession();
        assertThatThrownBy(() -> remove.remove(new RemoveRatingRequest(SUPPORT_ID, "SUPPORT", RESERVATION_ID,
                "rating-unknown"))).isInstanceOf(RatingNotFoundException.class);
        assertThat(sessions.saves()).isEmpty();
    }
}
