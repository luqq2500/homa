package com.homa.rating.domain;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.rating.domain.RatingFixtures.AISYAH;
import static com.homa.rating.domain.RatingFixtures.AISYAH_PARTY;
import static com.homa.rating.domain.RatingFixtures.COMPLETED_AT;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_PARTY;
import static com.homa.rating.domain.RatingFixtures.PROTECTION_WINDOW_END;
import static com.homa.rating.domain.RatingFixtures.RESERVATION;
import static com.homa.rating.domain.RatingFixtures.WINDOW_CLOSES_AT;
import static com.homa.rating.domain.RatingFixtures.openSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.rating.domain.exception.RatingWindowClosedException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RatingSessionOpeningTest {

    @Test
    @Tag("RTG-2")
    @Tag("RTG-9")
    @DisplayName("Completed Saturday 14:00, Kemas Home's job opens ratings, and the window closes the following "
            + "Sunday at 14:00.")
    void theWindowOpensWhenTheJobIsCompletedAndClosesSevenDaysAfterTheProtectionWindowEnds() {
        RatingSession session = openSession();

        assertThat(session.reservationId()).isEqualTo(RESERVATION);
        assertThat(session.customerId()).isEqualTo(AISYAH);
        assertThat(session.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(session.window().opensAt()).isEqualTo(COMPLETED_AT);
        assertThat(session.window().closesAt()).isEqualTo(WINDOW_CLOSES_AT);
        assertThat(session.status()).isEqualTo(RatingSessionStatus.OPEN);
        assertThat(session.isRevealed()).isFalse();
        assertThat(session.revealedAt()).isEmpty();
        assertThat(session.ratings()).isEmpty();
        assertThat(session.version()).isZero();
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("Completed on Monday, after the Protection Window ended, the window still closes on the 13th.")
    void aLateCompletionKeepsTheSameClosingMoment() {
        RatingSession session = RatingSession.open(RESERVATION, AISYAH, KEMAS_HOME, at(2026, 12, 7, 9, 0),
                PROTECTION_WINDOW_END, REQUESTED_AT);

        assertThat(session.window().opensAt()).isEqualTo(at(2026, 12, 7, 9, 0));
        assertThat(session.window().closesAt()).isEqualTo(WINDOW_CLOSES_AT);
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("A job marked Completed after the window closed gets no ratings.")
    void aJobCompletedAfterTheWindowClosedGetsNoRatings() {
        assertThatThrownBy(() -> RatingSession.open(RESERVATION, AISYAH, KEMAS_HOME, WINDOW_CLOSES_AT,
                PROTECTION_WINDOW_END, REQUESTED_AT))
                .isInstanceOf(RatingWindowClosedException.class)
                .hasMessageContaining("RTG-2");
        assertThatThrownBy(() -> RatingSession.open(RESERVATION, AISYAH, KEMAS_HOME, at(2026, 12, 14, 9, 0),
                PROTECTION_WINDOW_END, REQUESTED_AT))
                .isInstanceOf(RatingWindowClosedException.class);
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("A job marked Completed one minute before the window closes still opens ratings.")
    void aJobCompletedJustBeforeTheWindowClosesOpensRatings() {
        RatingSession session = RatingSession.open(RESERVATION, AISYAH, KEMAS_HOME,
                WINDOW_CLOSES_AT.minus(Duration.ofMinutes(1)), PROTECTION_WINDOW_END, REQUESTED_AT);

        assertThat(session.window().closesAt()).isEqualTo(WINDOW_CLOSES_AT);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("A later Rating Window leaves Aisyah's reservation, Requested on Monday 20:00, on the 7 days it was "
            + "booked with; a reservation Requested after the change gets 10.")
    void aLaterVersionLeavesAnEarlierReservationOnTheOldWindow() {
        Instant changedFrom = at(2026, 12, 1, 0, 0);
        RatingValues values = new RatingValues(List.of(RatingValues.current(),
                new RatingValues.Version(changedFrom, Duration.ofDays(10))));

        RatingSession aisyahs = RatingSession.open(RESERVATION, AISYAH, KEMAS_HOME, COMPLETED_AT,
                PROTECTION_WINDOW_END, REQUESTED_AT, values);
        RatingSession later = RatingSession.open(new ReservationId("reservation-later"), AISYAH, KEMAS_HOME,
                COMPLETED_AT, PROTECTION_WINDOW_END, changedFrom, values);

        assertThat(aisyahs.window().closesAt()).isEqualTo(WINDOW_CLOSES_AT);
        assertThat(later.window().closesAt()).isEqualTo(at(2026, 12, 16, 14, 0));
    }

    @Test
    @Tag("RTG-1")
    @DisplayName("The session knows its two sides: Aisyah is the customer and Kemas Home the provider.")
    void theSessionKnowsItsTwoSides() {
        RatingSession session = openSession();

        assertThat(session.roleOf(AISYAH.value())).contains(PartyRole.CUSTOMER);
        assertThat(session.roleOf(KEMAS_HOME.value())).contains(PartyRole.PROVIDER);
        assertThat(session.roleOf("customer-daniel")).isEmpty();
        assertThat(session.partyOf(PartyRole.CUSTOMER)).isEqualTo(AISYAH_PARTY);
        assertThat(session.partyOf(PartyRole.PROVIDER)).isEqualTo(KEMAS_HOME_PARTY);
    }

    @Test
    @Tag("RTG-9")
    @DisplayName("A session needs its reservation, both parties and the moments it opens from.")
    void aSessionNeedsItsParts() {
        assertThatThrownBy(() -> RatingSession.open(null, AISYAH, KEMAS_HOME, COMPLETED_AT, PROTECTION_WINDOW_END,
                REQUESTED_AT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RatingSession.open(RESERVATION, null, KEMAS_HOME, COMPLETED_AT, PROTECTION_WINDOW_END,
                REQUESTED_AT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RatingSession.open(RESERVATION, AISYAH, null, COMPLETED_AT, PROTECTION_WINDOW_END,
                REQUESTED_AT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RatingSession.open(RESERVATION, AISYAH, KEMAS_HOME, null, PROTECTION_WINDOW_END,
                REQUESTED_AT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RatingSession.open(RESERVATION, AISYAH, KEMAS_HOME, COMPLETED_AT, null,
                REQUESTED_AT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RatingSession.open(RESERVATION, AISYAH, KEMAS_HOME, COMPLETED_AT,
                PROTECTION_WINDOW_END, null)).isInstanceOf(NullPointerException.class);
    }
}
