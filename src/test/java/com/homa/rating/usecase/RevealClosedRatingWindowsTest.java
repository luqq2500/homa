package com.homa.rating.usecase;

import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.RESERVATION_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.WINDOW_CLOSES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.openSession;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProviderRatingRevealedIE;
import com.homa.rating.domain.Rating;
import com.homa.rating.domain.RatingId;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.RatingSessionStatus;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.domain.Stars;
import com.homa.rating.usecase.dto.RevealClosedRatingWindowsRequest;
import com.homa.rating.usecase.dto.RevealClosedRatingWindowsResponse;
import com.homa.rating.usecase.port.out.RatingSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RevealClosedRatingWindowsTest {

    private static final RevealClosedRatingWindowsRequest RUN = new RevealClosedRatingWindowsRequest();

    private final InMemoryRatingSessionRepository sessions = new InMemoryRatingSessionRepository();
    private final RecordingRatingIEPublisher publisher = new RecordingRatingIEPublisher();
    private final InlineUnitOfWork unitOfWork = new InlineUnitOfWork();
    private final FixedTimeSource time = new FixedTimeSource(WINDOW_CLOSES_AT);
    private final RatingEvents events = new RatingEvents(new SequentialEventIds(), time);
    private final ProviderRatingUpdates providerRatings = new ProviderRatingUpdates(sessions, publisher, events);
    private final RevealClosedRatingWindows reveal =
            new RevealClosedRatingWindows(sessions, providerRatings, unitOfWork, time);

    private static Rating byAisyah(int stars) {
        return Rating.by(RatingId.generate(), AISYAH, new Stars(stars), null,
                Map.of(), List.of(), AISYAH_RATES_AT);
    }

    private static Rating byKemasHome(int stars) {
        return Rating.by(RatingId.generate(), KEMAS_HOME, new Stars(stars), null,
                Map.of(), List.of(), KEMAS_HOME_RATES_AT);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("Aisyah rated on Saturday and Kemas Home never did: when the window closes on Sunday 13 December "
            + "14:00 her rating shows, and Listing is told.")
    void aClosedWindowRevealsAnUnansweredRating() {
        RatingSession session = openSession();
        session.submit(byAisyah(4));
        sessions.given(session);

        RevealClosedRatingWindowsResponse response = reveal.revealClosed(RUN);

        assertThat(response.revealedReservationIds()).containsExactly(RESERVATION_ID);
        assertThat(response.changedMeanwhileIds()).isEmpty();
        assertThat(session.status()).isEqualTo(RatingSessionStatus.REVEALED);
        assertThat(session.revealedAt()).contains(WINDOW_CLOSES_AT);
        assertThat(sessions.saves()).containsExactly(session);
        assertThat(publisher.published()).containsExactly(new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 1,
                "4.0", null, null, null, WINDOW_CLOSES_AT));
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("One minute before the window closes nothing is revealed: the ratings stay hidden.")
    void nothingIsRevealedWhileTheWindowIsOpen() {
        RatingSession session = openSession();
        session.submit(byAisyah(4));
        sessions.given(session);
        time.set(WINDOW_CLOSES_AT.minus(Duration.ofMinutes(1)));

        RevealClosedRatingWindowsResponse response = reveal.revealClosed(RUN);

        assertThat(response.revealedReservationIds()).isEmpty();
        assertThat(session.status()).isEqualTo(RatingSessionStatus.OPEN);
        assertThat(sessions.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(unitOfWork.units()).isZero();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("Kemas Home rated Aisyah and she never rated it: its rating of her is revealed, and nothing about "
            + "Kemas Home's page changes, so nothing is published.")
    void aRevealOfOnlyTheProvidersRatingPublishesNothing() {
        RatingSession session = openSession();
        session.submit(byKemasHome(5));
        sessions.given(session);

        RevealClosedRatingWindowsResponse response = reveal.revealClosed(RUN);

        assertThat(response.revealedReservationIds()).containsExactly(RESERVATION_ID);
        assertThat(session.isRevealed()).isTrue();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("Nobody rated: the window closing settles the session with nothing to show or publish.")
    void aClosedWindowWithNoRatingsRevealsNothing() {
        RatingSession session = openSession();
        sessions.given(session);

        RevealClosedRatingWindowsResponse response = reveal.revealClosed(RUN);

        assertThat(response.revealedReservationIds()).containsExactly(RESERVATION_ID);
        assertThat(session.isRevealed()).isTrue();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("A session revealed when both rated is left alone, and a second run reveals nothing again.")
    void revealedSessionsAreNotRevealedAgain() {
        RatingSession both = openSession();
        both.submit(byAisyah(4));
        both.submit(byKemasHome(5));
        RatingSession late = openSession("reservation-late");
        sessions.given(both);
        sessions.given(late);

        RevealClosedRatingWindowsResponse first = reveal.revealClosed(RUN);
        RevealClosedRatingWindowsResponse second = reveal.revealClosed(RUN);

        assertThat(first.revealedReservationIds()).containsExactly("reservation-late");
        assertThat(second.revealedReservationIds()).isEmpty();
        assertThat(both.revealedAt()).contains(KEMAS_HOME_RATES_AT);
        assertThat(sessions.saves()).containsExactly(late);
        assertThat(unitOfWork.units()).isEqualTo(1);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("Each closed session is revealed in a unit of work of its own.")
    void eachSessionIsRevealedInItsOwnUnitOfWork() {
        RatingSession first = openSession();
        first.submit(byAisyah(4));
        RatingSession second = openSession("reservation-second");
        sessions.given(first);
        sessions.given(second);

        RevealClosedRatingWindowsResponse response = reveal.revealClosed(RUN);

        assertThat(response.revealedReservationIds()).containsExactlyInAnyOrder(RESERVATION_ID, "reservation-second");
        assertThat(unitOfWork.units()).isEqualTo(2);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("Someone saved a session first: that one is left for the next run and the others are still "
            + "revealed.")
    void aSessionChangedMeanwhileDoesNotStopTheOthers() {
        RatingSession first = openSession();
        first.submit(byAisyah(4));
        RatingSession second = openSession("reservation-second");
        sessions.given(first);
        sessions.given(second);
        sessions.conflictOnNextSave(new ReservationId(RESERVATION_ID));

        RevealClosedRatingWindowsResponse response = reveal.revealClosed(RUN);

        assertThat(response.changedMeanwhileIds()).containsExactly(RESERVATION_ID);
        assertThat(response.revealedReservationIds()).containsExactly("reservation-second");
        assertThat(sessions.saves()).containsExactly(second);
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("A session that is gone by the time it is loaded is skipped.")
    void aSessionThatVanishedIsSkipped() {
        RatingSessionRepository vanishing = new InMemoryRatingSessionRepository() {
            @Override
            public List<ReservationId> findIdsOfOpenWithWindowClosedBy(Instant now) {
                return List.of(new ReservationId("reservation-purged"));
            }
        };
        RevealClosedRatingWindows run = new RevealClosedRatingWindows(vanishing,
                new ProviderRatingUpdates(vanishing, publisher, events), unitOfWork, time);

        RevealClosedRatingWindowsResponse response = run.revealClosed(RUN);

        assertThat(response.revealedReservationIds()).isEmpty();
        assertThat(response.changedMeanwhileIds()).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("The scheduler runs an hour late: the reveal is still stamped with the closing moment, and the event "
            + "with the time it was published.")
    void aLateRunStampsTheClosingMoment() {
        RatingSession session = openSession();
        session.submit(byAisyah(2));
        sessions.given(session);
        Instant lateRun = WINDOW_CLOSES_AT.plus(Duration.ofHours(1));
        time.set(lateRun);

        reveal.revealClosed(RUN);

        assertThat(session.revealedAt()).contains(WINDOW_CLOSES_AT);
        assertThat(publisher.published()).containsExactly(new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 1,
                "2.0", null, null, null, lateRun));
    }

    @Test
    @Tag("RTG-6")
    @DisplayName("Aisyah's 2 stars and her next reservation's 5 are revealed in one run: the second event carries both "
            + "ratings, averaging 3.5.")
    void eachEventCarriesEveryRevealedRatingOfTheProviderSoFar() {
        RatingSession first = openSession();
        first.submit(byAisyah(2));
        RatingSession second = openSession("reservation-second");
        second.submit(byAisyah(5));
        sessions.given(first);
        sessions.given(second);

        reveal.revealClosed(RUN);

        assertThat(publisher.published()).containsExactly(
                new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 1, "2.0", null, null, null, WINDOW_CLOSES_AT),
                new ProviderRatingRevealedIE("event-2", KEMAS_HOME_ID, 2, "3.5", null, null, null, WINDOW_CLOSES_AT));
    }
}
