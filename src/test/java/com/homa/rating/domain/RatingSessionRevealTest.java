package com.homa.rating.domain;

import static com.homa.rating.domain.RatingFixtures.AISYAH_PARTY;
import static com.homa.rating.domain.RatingFixtures.AISYAH_RATES_AT;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_PARTY;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.domain.RatingFixtures.WINDOW_CLOSES_AT;
import static com.homa.rating.domain.RatingFixtures.aisyahRates;
import static com.homa.rating.domain.RatingFixtures.kemasHomeRates;
import static com.homa.rating.domain.RatingFixtures.openSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.rating.domain.exception.RatingSessionNotOpenException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RatingSessionRevealTest {

    @Test
    @Tag("RTG-3")
    @DisplayName("Aisyah rates and Kemas Home never does: her rating shows when the window closes.")
    void theWindowClosingRevealsAnUnansweredRating() {
        RatingSession session = openSession();
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);
        session.submit(aisyahs);

        boolean revealed = session.revealIfWindowClosed(WINDOW_CLOSES_AT);

        assertThat(revealed).isTrue();
        assertThat(session.status()).isEqualTo(RatingSessionStatus.REVEALED);
        assertThat(session.revealedAt()).contains(WINDOW_CLOSES_AT);
        assertThat(session.revealedRatingAbout(KEMAS_HOME_PARTY)).contains(aisyahs);
        assertThat(session.revealedRatingAbout(AISYAH_PARTY)).isEmpty();
        assertThat(session.showsRatingOfProvider()).isTrue();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("The scheduler runs an hour late: the reveal is still stamped with the closing moment.")
    void aLateSchedulerStillStampsTheClosingMoment() {
        RatingSession session = openSession();

        boolean revealed = session.revealIfWindowClosed(WINDOW_CLOSES_AT.plus(Duration.ofHours(1)));

        assertThat(revealed).isTrue();
        assertThat(session.revealedAt()).contains(WINDOW_CLOSES_AT);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("One minute before the window closes nothing is revealed; the ratings stay hidden.")
    void nothingIsRevealedWhileTheWindowIsOpen() {
        RatingSession session = openSession();
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);
        session.submit(aisyahs);

        boolean revealed = session.revealIfWindowClosed(WINDOW_CLOSES_AT.minus(Duration.ofMinutes(1)));

        assertThat(revealed).isFalse();
        assertThat(session.status()).isEqualTo(RatingSessionStatus.OPEN);
        assertThat(session.revealedAt()).isEmpty();
        assertThat(session.revealedRatingAbout(KEMAS_HOME_PARTY)).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("When nobody rated, the window closing reveals nothing to show, and the session is done.")
    void aClosedWindowWithNoRatingsRevealsNothing() {
        RatingSession session = openSession();

        assertThat(session.revealIfWindowClosed(WINDOW_CLOSES_AT)).isTrue();

        assertThat(session.isRevealed()).isTrue();
        assertThat(session.ratings()).isEmpty();
        assertThat(session.showsRatingOfProvider()).isFalse();
        assertThatThrownBy(() -> session.submit(aisyahRates(4, WINDOW_CLOSES_AT.minusSeconds(1))))
                .isInstanceOf(RatingSessionNotOpenException.class);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("A session already revealed when both rated isn't revealed again by the closing window.")
    void anAlreadyRevealedSessionStaysAsItWas() {
        RatingSession session = RatingFixtures.revealedSession(aisyahRates(4, AISYAH_RATES_AT),
                kemasHomeRates(5, KEMAS_HOME_RATES_AT));

        assertThat(session.revealIfWindowClosed(WINDOW_CLOSES_AT.plus(Duration.ofDays(1)))).isFalse();

        assertThat(session.revealedAt()).contains(KEMAS_HOME_RATES_AT);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("A voided session is never revealed.")
    void aVoidSessionIsNeverRevealed() {
        RatingSession session = openSession();
        session.voidForProviderNoShow();

        assertThat(session.revealIfWindowClosed(WINDOW_CLOSES_AT)).isFalse();

        assertThat(session.status()).isEqualTo(RatingSessionStatus.VOID);
        assertThat(session.revealedAt()).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("Double-blind: only a revealed session shows a rating, and each rating is shown about the party "
            + "the other side rated.")
    void aRatingIsShownAboutTheOtherSideOnlyOnceRevealed() {
        RatingSession session = RatingFixtures.revealedSession(aisyahRates(4, AISYAH_RATES_AT),
                kemasHomeRates(2, KEMAS_HOME_RATES_AT));
        Party stranger = Party.customer(new CustomerId("customer-daniel"));

        assertThat(session.revealedRatingAbout(KEMAS_HOME_PARTY).orElseThrow().stars()).isEqualTo(new Stars(4));
        assertThat(session.revealedRatingAbout(AISYAH_PARTY).orElseThrow().stars()).isEqualTo(new Stars(2));
        assertThat(session.revealedRatingAbout(stranger)).isEmpty();
    }
}
