package com.homa.rating.domain;

import static com.homa.rating.domain.RatingFixtures.AISYAH;
import static com.homa.rating.domain.RatingFixtures.AISYAH_RATES_AT;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.domain.RatingFixtures.RESERVATION;
import static com.homa.rating.domain.RatingFixtures.WINDOW_CLOSES_AT;
import static com.homa.rating.domain.RatingFixtures.aisyahRates;
import static com.homa.rating.domain.RatingFixtures.kemasHomeRates;
import static com.homa.rating.domain.RatingFixtures.openSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredRatingSessionTest {

    private static final RatingWindow WINDOW = openSession().window();

    private static RatingSession restore(List<Rating> ratings, RatingSessionStatus status, java.time.Instant revealedAt) {
        return RatingSession.restore(RESERVATION, AISYAH, KEMAS_HOME, WINDOW, ratings, status, revealedAt, 3);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("A revealed session comes back as stored, with the version it was saved at.")
    void aRevealedSessionComesBack() {
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);
        Rating kemasHomes = kemasHomeRates(5, KEMAS_HOME_RATES_AT);

        RatingSession session = restore(List.of(aisyahs, kemasHomes), RatingSessionStatus.REVEALED, KEMAS_HOME_RATES_AT);

        assertThat(session.reservationId()).isEqualTo(RESERVATION);
        assertThat(session.customerId()).isEqualTo(AISYAH);
        assertThat(session.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(session.window()).isEqualTo(WINDOW);
        assertThat(session.ratings()).containsExactly(aisyahs, kemasHomes);
        assertThat(session.status()).isEqualTo(RatingSessionStatus.REVEALED);
        assertThat(session.revealedAt()).contains(KEMAS_HOME_RATES_AT);
        assertThat(session.version()).isEqualTo(3);
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("An open session with one rating, and a void one with none, come back as stored.")
    void openAndVoidSessionsComeBack() {
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);

        RatingSession open = restore(List.of(aisyahs), RatingSessionStatus.OPEN, null);
        RatingSession empty = restore(List.of(), RatingSessionStatus.OPEN, null);
        RatingSession voided = restore(List.of(), RatingSessionStatus.VOID, KEMAS_HOME_RATES_AT);
        RatingSession voidedUnrevealed = restore(List.of(), RatingSessionStatus.VOID, null);

        assertThat(open.ratingBy(PartyRole.CUSTOMER)).contains(aisyahs);
        assertThat(open.status()).isEqualTo(RatingSessionStatus.OPEN);
        assertThat(empty.ratings()).isEmpty();
        assertThat(voided.status()).isEqualTo(RatingSessionStatus.VOID);
        assertThat(voidedUnrevealed.revealedAt()).isEmpty();
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("A revealed session with no ratings, when nobody rated, comes back as stored.")
    void aRevealedSessionWithNoRatingsComesBack() {
        RatingSession session = restore(List.of(), RatingSessionStatus.REVEALED, WINDOW_CLOSES_AT);

        assertThat(session.isRevealed()).isTrue();
    }

    @Test
    @Tag("RTG-1")
    @DisplayName("A stored session can't hold two ratings from one side, or a rating by someone who isn't a party.")
    void aStoredShapeNeedsOneRatingPerSideByItsParty() {
        Rating first = aisyahRates(4, AISYAH_RATES_AT);
        Rating second = aisyahRates(3, KEMAS_HOME_RATES_AT);
        Rating stranger = Rating.by(RatingId.generate(), Party.customer(new CustomerId("customer-daniel")), new Stars(5),
                null, java.util.Map.of(), List.of(), AISYAH_RATES_AT);

        assertThatThrownBy(() -> restore(List.of(first, second), RatingSessionStatus.OPEN, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("one rating per side");
        assertThatThrownBy(() -> restore(List.of(stranger), RatingSessionStatus.OPEN, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("party");
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("A stored status needs to fit its parts: open reveals nothing and holds at most one rating, "
            + "revealed has its moment, void holds no rating.")
    void aStoredStatusFitsItsParts() {
        Rating aisyahs = aisyahRates(4, AISYAH_RATES_AT);
        Rating kemasHomes = kemasHomeRates(5, KEMAS_HOME_RATES_AT);

        assertThatThrownBy(() -> restore(List.of(aisyahs), RatingSessionStatus.OPEN, KEMAS_HOME_RATES_AT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("open");
        assertThatThrownBy(() -> restore(List.of(aisyahs, kemasHomes), RatingSessionStatus.OPEN, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("open");
        assertThatThrownBy(() -> restore(List.of(aisyahs), RatingSessionStatus.REVEALED, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reveal");
        assertThatThrownBy(() -> restore(List.of(aisyahs), RatingSessionStatus.VOID, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("void");
        assertThatThrownBy(() -> RatingSession.restore(RESERVATION, AISYAH, KEMAS_HOME, WINDOW, List.of(), null, null, 0))
                .isInstanceOf(NullPointerException.class);
    }
}
