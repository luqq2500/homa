package com.homa.rating.domain;

import static com.homa.rating.domain.RatingFixtures.AISYAH_PARTY;
import static com.homa.rating.domain.RatingFixtures.AISYAH_RATES_AT;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_PARTY;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.domain.RatingFixtures.RESERVATION;
import static com.homa.rating.domain.RatingFixtures.aisyahRates;
import static com.homa.rating.domain.RatingFixtures.kemasHomeRates;
import static com.homa.rating.domain.RatingFixtures.openSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.rating.domain.exception.RatingNotFoundException;
import com.homa.rating.domain.exception.RatingSessionNotOpenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RatingSessionVoidAndRemoveTest {

    @Test
    @Tag("NSH-6")
    @DisplayName("Kemas Home never came but marked the job Completed with an old photo; both ratings were revealed. "
            + "Support confirms the Provider No Show: both ratings are removed.")
    void aProviderNoShowRemovesBothRevealedRatings() {
        RatingSession session = RatingFixtures.revealedSession(aisyahRates(4, AISYAH_RATES_AT),
                kemasHomeRates(5, KEMAS_HOME_RATES_AT));
        assertThat(session.showsRatingOfProvider()).isTrue();

        boolean changed = session.voidForProviderNoShow();

        assertThat(changed).isTrue();
        assertThat(session.status()).isEqualTo(RatingSessionStatus.VOID);
        assertThat(session.ratings()).isEmpty();
        assertThat(session.revealedRatingAbout(KEMAS_HOME_PARTY)).isEmpty();
        assertThat(session.revealedRatingAbout(AISYAH_PARTY)).isEmpty();
        assertThat(session.showsRatingOfProvider()).isFalse();
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("A Provider No Show voids a session whose ratings were still hidden, and nothing more is taken.")
    void aProviderNoShowVoidsHiddenRatingsToo() {
        RatingSession session = openSession();
        session.submit(aisyahRates(4, AISYAH_RATES_AT));

        assertThat(session.voidForProviderNoShow()).isTrue();

        assertThat(session.ratings()).isEmpty();
        assertThatThrownBy(() -> session.submit(kemasHomeRates(5, KEMAS_HOME_RATES_AT)))
                .isInstanceOf(RatingSessionNotOpenException.class).hasMessageContaining("VOID");
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("Voiding a session that is already void changes nothing.")
    void voidingTwiceChangesNothing() {
        RatingSession session = openSession();
        session.voidForProviderNoShow();

        assertThat(session.voidForProviderNoShow()).isFalse();

        assertThat(session.status()).isEqualTo(RatingSessionStatus.VOID);
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Support removes a revealed rating that includes a customer's phone number: the other rating stays.")
    void supportRemovesOneRevealedRating() {
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);
        Rating kemasHomes = kemasHomeRates(2, KEMAS_HOME_RATES_AT);
        RatingSession session = RatingFixtures.revealedSession(aisyahs, kemasHomes);

        Rating removed = session.remove(kemasHomes.id());

        assertThat(removed).isSameAs(kemasHomes);
        assertThat(session.ratings()).containsExactly(aisyahs);
        assertThat(session.revealedRatingAbout(AISYAH_PARTY)).isEmpty();
        assertThat(session.revealedRatingAbout(KEMAS_HOME_PARTY)).contains(aisyahs);
        assertThat(session.status()).isEqualTo(RatingSessionStatus.REVEALED);
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Support removes the customer's revealed rating: Kemas Home's page no longer shows it.")
    void removingTheCustomersRatingStopsShowingItOnTheProvider() {
        Rating aisyahs = aisyahRates(1, AISYAH_RATES_AT);
        RatingSession session = RatingFixtures.revealedSession(aisyahs, kemasHomeRates(5, KEMAS_HOME_RATES_AT));

        session.remove(aisyahs.id());

        assertThat(session.showsRatingOfProvider()).isFalse();
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Support removes a hidden rating before it is revealed: that side may rate again while the window "
            + "is open.")
    void aHiddenRatingCanBeRemovedAndTheSideRatesAgain() {
        RatingSession session = openSession();
        Rating first = aisyahRates(1, AISYAH_RATES_AT);
        session.submit(first);

        session.remove(first.id());
        Rating second = aisyahRates(4, KEMAS_HOME_RATES_AT);
        session.submit(second);

        assertThat(session.ratings()).containsExactly(second);
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Support can't remove a rating the reservation doesn't have, or one already removed.")
    void aRatingThatIsNotThereCantBeRemoved() {
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);
        RatingSession session = openSession();
        session.submit(aisyahs);
        RatingId unknown = new RatingId("rating-unknown");

        assertThatThrownBy(() -> session.remove(unknown)).isInstanceOf(RatingNotFoundException.class)
                .hasMessageContaining("RTG-11").hasMessageContaining(RESERVATION.value())
                .hasMessageContaining("rating-unknown");
        session.remove(aisyahs.id());
        assertThatThrownBy(() -> session.remove(aisyahs.id())).isInstanceOf(RatingNotFoundException.class);
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("After a Provider No Show there is no rating left for Support to remove.")
    void aVoidSessionHasNothingToRemove() {
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);
        RatingSession session = openSession();
        session.submit(aisyahs);
        session.voidForProviderNoShow();

        assertThatThrownBy(() -> session.remove(aisyahs.id())).isInstanceOf(RatingNotFoundException.class);
    }
}
