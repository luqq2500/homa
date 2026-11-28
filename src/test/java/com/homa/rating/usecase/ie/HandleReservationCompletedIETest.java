package com.homa.rating.usecase.ie;

import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.COMPLETED_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.PROTECTION_WINDOW_END;
import static com.homa.rating.usecase.RatingUsecaseFixtures.RESERVATION_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.WINDOW_CLOSES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.completed;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.RatingSessionStatus;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.usecase.InMemoryProcessedMessageStore;
import com.homa.rating.usecase.InMemoryRatingSessionRepository;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationCompletedIETest {

    private final InMemoryRatingSessionRepository sessions = new InMemoryRatingSessionRepository();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final HandleReservationCompletedIE handler = new HandleReservationCompletedIE(sessions, processed);

    @Test
    @Tag("RTG-2")
    @Tag("RTG-9")
    @DisplayName("Kemas Home marks the job Completed on Saturday 14:00: ratings open between Aisyah and Kemas Home, "
            + "until the following Sunday 14:00.")
    void completedJobOpensRatings() {
        handler.handle(completed("reservation-event-5"));

        RatingSession session = sessions.findByReservationId(new ReservationId(RESERVATION_ID)).orElseThrow();
        assertThat(session.customerId().value()).isEqualTo(AISYAH_ID);
        assertThat(session.providerId().value()).isEqualTo(KEMAS_HOME_ID);
        assertThat(session.window().opensAt()).isEqualTo(COMPLETED_AT);
        assertThat(session.window().closesAt()).isEqualTo(WINDOW_CLOSES_AT);
        assertThat(session.status()).isEqualTo(RatingSessionStatus.OPEN);
        assertThat(session.ratings()).isEmpty();
        assertThat(sessions.saves()).containsExactly(session);
        assertThat(processed.recorded()).containsExactly("reservation-event-5|rating.ReservationCompleted");
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("Completed on Monday, after the Protection Window ended: the window still closes on the 13th.")
    void aLateCompletionKeepsTheClosingMoment() {
        handler.handle(completed("reservation-event-5", RESERVATION_ID, at(2026, 12, 7, 9, 0)));

        RatingSession session = sessions.findByReservationId(new ReservationId(RESERVATION_ID)).orElseThrow();
        assertThat(session.window().opensAt()).isEqualTo(at(2026, 12, 7, 9, 0));
        assertThat(session.window().closesAt()).isEqualTo(WINDOW_CLOSES_AT);
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("A redelivered Completed opens nothing a second time.")
    void redeliveredEventChangesNothing() {
        handler.handle(completed("reservation-event-5"));

        handler.handle(completed("reservation-event-5"));

        assertThat(sessions.saves()).hasSize(1);
        assertThat(processed.recorded()).hasSize(1);
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("An event this handler already handled is not applied again, whatever else is stored.")
    void alreadyHandledEventIsNotApplied() {
        processed.recordFirstHandling("reservation-event-5", HandleReservationCompletedIE.HANDLER);

        handler.handle(completed("reservation-event-5"));

        assertThat(sessions.all()).isEmpty();
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("A second Completed for a reservation that already has its ratings leaves them as they are.")
    void aSessionThatAlreadyExistsIsLeftAlone() {
        handler.handle(completed("reservation-event-5"));
        RatingSession first = sessions.findByReservationId(new ReservationId(RESERVATION_ID)).orElseThrow();

        handler.handle(completed("reservation-event-6", RESERVATION_ID, COMPLETED_AT.plus(Duration.ofHours(2))));

        assertThat(sessions.saves()).hasSize(1);
        assertThat(sessions.findByReservationId(new ReservationId(RESERVATION_ID))).containsSame(first);
        assertThat(first.window().opensAt()).isEqualTo(COMPLETED_AT);
        assertThat(processed.recorded()).hasSize(2);
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("A job marked Completed after the window closed gets no ratings, and the event is not retried "
            + "forever.")
    void aJobCompletedAfterTheWindowClosedGetsNoRatings() {
        handler.handle(completed("reservation-event-5", RESERVATION_ID, WINDOW_CLOSES_AT));

        assertThat(sessions.all()).isEmpty();
        assertThat(processed.recorded()).containsExactly("reservation-event-5|rating.ReservationCompleted");
    }

    @Test
    @Tag("RTG-2")
    @DisplayName("A job marked Completed one minute before the window would close still opens ratings.")
    void aJobCompletedJustBeforeTheWindowClosesOpensRatings() {
        handler.handle(completed("reservation-event-5", RESERVATION_ID, WINDOW_CLOSES_AT.minus(Duration.ofMinutes(1))));

        assertThat(sessions.all()).hasSize(1);
    }

    @Test
    @Tag("RES-8")
    @DisplayName("The window is the one in force when Aisyah's reservation was Requested on Monday 20:00.")
    void theWindowComesFromTheValuesWhenRequested() {
        assertThat(completed("reservation-event-5").requestedAt()).isEqualTo(REQUESTED_AT);
        assertThat(completed("reservation-event-5").protectionWindowEnd()).isEqualTo(PROTECTION_WINDOW_END);

        handler.handle(completed("reservation-event-5"));

        assertThat(sessions.all().get(0).window().closesAt()).isEqualTo(WINDOW_CLOSES_AT);
    }
}
