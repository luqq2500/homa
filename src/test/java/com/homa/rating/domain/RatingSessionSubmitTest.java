package com.homa.rating.domain;

import static com.homa.rating.domain.RatingFixtures.AISYAH_PARTY;
import static com.homa.rating.domain.RatingFixtures.AISYAH_RATES_AT;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_PARTY;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.domain.RatingFixtures.RESERVATION;
import static com.homa.rating.domain.RatingFixtures.WINDOW_CLOSES_AT;
import static com.homa.rating.domain.RatingFixtures.aisyahRates;
import static com.homa.rating.domain.RatingFixtures.kemasHomeRates;
import static com.homa.rating.domain.RatingFixtures.openSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.rating.domain.exception.RatingAlreadySubmittedException;
import com.homa.rating.domain.exception.RatingAuthorNotAPartyException;
import com.homa.rating.domain.exception.RatingSessionNotOpenException;
import com.homa.rating.domain.exception.RatingWindowClosedException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RatingSessionSubmitTest {

    @Test
    @Tag("RTG-1")
    @Tag("RTG-3")
    @DisplayName("Aisyah rates Kemas Home on Saturday evening: her rating is held, and nothing is revealed yet.")
    void aRatingIsHeldAndHiddenUntilTheOtherSideRates() {
        RatingSession session = openSession();
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);

        session.submit(aisyahs);

        assertThat(session.ratingBy(PartyRole.CUSTOMER)).contains(aisyahs);
        assertThat(session.ratingBy(PartyRole.PROVIDER)).isEmpty();
        assertThat(session.ratings()).containsExactly(aisyahs);
        assertThat(session.status()).isEqualTo(RatingSessionStatus.OPEN);
        assertThat(session.revealedAt()).isEmpty();
        assertThat(session.revealedRatingAbout(KEMAS_HOME_PARTY)).isEmpty();
        assertThat(session.revealedRatingAbout(AISYAH_PARTY)).isEmpty();
        assertThat(session.showsRatingOfProvider()).isFalse();
    }

    @Test
    @Tag("RTG-1")
    @Tag("RTG-3")
    @DisplayName("Aisyah rates on Saturday and Kemas Home on Monday: both show on Monday, at the moment the second "
            + "came in.")
    void bothRatingsAreRevealedWhenTheSecondComesIn() {
        RatingSession session = openSession();
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);
        Rating kemasHomes = kemasHomeRates(5, KEMAS_HOME_RATES_AT);
        session.submit(aisyahs);

        session.submit(kemasHomes);

        assertThat(session.status()).isEqualTo(RatingSessionStatus.REVEALED);
        assertThat(session.isRevealed()).isTrue();
        assertThat(session.revealedAt()).contains(KEMAS_HOME_RATES_AT);
        assertThat(session.revealedRatingAbout(KEMAS_HOME_PARTY)).contains(aisyahs);
        assertThat(session.revealedRatingAbout(AISYAH_PARTY)).contains(kemasHomes);
        assertThat(session.showsRatingOfProvider()).isTrue();
        assertThat(session.ratings()).containsExactly(aisyahs, kemasHomes);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("The order doesn't matter: Kemas Home rates first, Aisyah second, and both show when she does.")
    void theProviderCanRateFirst() {
        RatingSession session = openSession();
        Rating kemasHomes = kemasHomeRates(5, AISYAH_RATES_AT);
        Rating aisyahs = aisyahRates(4, KEMAS_HOME_RATES_AT);

        session.submit(kemasHomes);
        assertThat(session.isRevealed()).isFalse();
        session.submit(aisyahs);

        assertThat(session.revealedAt()).contains(KEMAS_HOME_RATES_AT);
        assertThat(session.revealedRatingAbout(AISYAH_PARTY)).contains(kemasHomes);
    }

    @Test
    @Tag("RTG-1")
    @DisplayName("Aisyah rates only once: a second rating from her is refused.")
    void eachSideRatesOnce() {
        RatingSession session = openSession();
        session.submit(aisyahRates(4, AISYAH_RATES_AT));

        assertThatThrownBy(() -> session.submit(aisyahRates(1, AISYAH_RATES_AT.plusSeconds(60))))
                .isInstanceOf(RatingAlreadySubmittedException.class)
                .hasMessageContaining("RTG-1")
                .hasMessageContaining("CUSTOMER")
                .hasMessageContaining(RESERVATION.value());
        assertThat(session.ratingBy(PartyRole.CUSTOMER).orElseThrow().stars()).isEqualTo(new Stars(4));
    }

    @Test
    @Tag("RTG-1")
    @DisplayName("A stranger can't rate Aisyah's reservation: Daniel is not its customer, Hana Cleaning not its "
            + "provider.")
    void onlyThePartiesOfTheReservationRate() {
        RatingSession session = openSession();
        Rating daniel = Rating.by(RatingId.generate(), Party.customer(new CustomerId("customer-daniel")), new Stars(5),
                null, Map.of(), List.of(), AISYAH_RATES_AT);
        Rating hana = Rating.by(RatingId.generate(), Party.provider(new ProviderId("provider-hana")), new Stars(5),
                null, Map.of(), List.of(), AISYAH_RATES_AT);

        assertThatThrownBy(() -> session.submit(daniel)).isInstanceOf(RatingAuthorNotAPartyException.class)
                .hasMessageContaining("RTG-1").hasMessageContaining("customer-daniel");
        assertThatThrownBy(() -> session.submit(hana)).isInstanceOf(RatingAuthorNotAPartyException.class);
        assertThat(session.ratings()).isEmpty();
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("One minute before the window closes Aisyah can still rate.")
    void aRatingOneMinuteBeforeTheWindowClosesIsTaken() {
        RatingSession session = openSession();

        session.submit(aisyahRates(4, WINDOW_CLOSES_AT.minus(Duration.ofMinutes(1))));

        assertThat(session.ratingBy(PartyRole.CUSTOMER)).isPresent();
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("One minute after the window closes, and at the closing moment itself, a rating is too late.")
    void aRatingAfterTheWindowClosesIsRefused() {
        RatingSession session = openSession();

        assertThatThrownBy(() -> session.submit(aisyahRates(4, WINDOW_CLOSES_AT.plus(Duration.ofMinutes(1)))))
                .isInstanceOf(RatingWindowClosedException.class).hasMessageContaining("RTG-2");
        assertThatThrownBy(() -> session.submit(aisyahRates(4, WINDOW_CLOSES_AT)))
                .isInstanceOf(RatingWindowClosedException.class);
        assertThat(session.ratings()).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("Once both ratings are revealed, nothing more is taken.")
    void aRevealedSessionTakesNoMoreRatings() {
        RatingSession session = RatingFixtures.revealedSession(aisyahRates(4, AISYAH_RATES_AT),
                kemasHomeRates(5, KEMAS_HOME_RATES_AT));
        assertThat(session.isRevealed()).isTrue();

        assertThatThrownBy(() -> session.submit(aisyahRates(1, KEMAS_HOME_RATES_AT)))
                .isInstanceOf(RatingSessionNotOpenException.class)
                .hasMessageContaining("REVEALED").hasMessageContaining(RESERVATION.value());
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("A rating needs to be there to be submitted.")
    void aRatingIsRequired() {
        assertThatThrownBy(() -> openSession().submit(null)).isInstanceOf(NullPointerException.class);
    }
}
