package com.homa.rating.usecase;

import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.CUSTOMER_CANCELLED_FREE;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.CUSTOMER_CANCELLED_WITH_FEE;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.PROVIDER_NO_SHOW;
import static com.homa.rating.usecase.RatingUsecaseFixtures.DANIEL_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.FRIDAY_22_00;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.RESERVATION_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.SUPPORT_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.WINDOW_CLOSES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.aisyahRates;
import static com.homa.rating.usecase.RatingUsecaseFixtures.cancelled;
import static com.homa.rating.usecase.RatingUsecaseFixtures.completed;
import static com.homa.rating.usecase.RatingUsecaseFixtures.kemasHomeRates;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProviderRatingRevealedIE;
import com.homa.ie.events.ReliabilityChangedIE;
import com.homa.rating.usecase.dto.RemoveRatingRequest;
import com.homa.rating.usecase.dto.RevealClosedRatingWindowsRequest;
import com.homa.rating.usecase.dto.SubmitRatingRequest;
import com.homa.rating.usecase.dto.SubmitRatingResponse;
import com.homa.rating.usecase.dto.ViewRatingsAndReliabilityRequest;
import com.homa.rating.usecase.dto.ViewRatingsAndReliabilityResponse;
import com.homa.rating.usecase.ie.HandleReservationCancelledIE;
import com.homa.rating.usecase.ie.HandleReservationCompletedIE;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Aisyah's standard reservation from Completed to counts, through every Rating use case wired together. */
class RatingFlowTest {

    private final InMemoryRatingSessionRepository sessions = new InMemoryRatingSessionRepository();
    private final InMemoryReliabilityRecordRepository reliability = new InMemoryReliabilityRecordRepository();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final RecordingRatingIEPublisher publisher = new RecordingRatingIEPublisher();
    private final FixedTimeSource time = new FixedTimeSource(RatingUsecaseFixtures.COMPLETED_AT);
    private final RatingEvents events = new RatingEvents(new SequentialEventIds(), time);
    private final ProviderRatingUpdates providerRatings = new ProviderRatingUpdates(sessions, publisher, events);

    private final HandleReservationCompletedIE onCompleted = new HandleReservationCompletedIE(sessions, processed);
    private final HandleReservationCancelledIE onCancelled = new HandleReservationCancelledIE(sessions, reliability,
            providerRatings, publisher, events, processed);
    private final PartySubmitRating submit = new PartySubmitRating(sessions, providerRatings, time);
    private final RevealClosedRatingWindows reveal =
            new RevealClosedRatingWindows(sessions, providerRatings, new InlineUnitOfWork(), time);
    private final SupportRemoveRating remove = new SupportRemoveRating(sessions, providerRatings);
    private final ViewRatingsAndReliability view = new ViewRatingsAndReliability(sessions, reliability, time);

    private ViewRatingsAndReliabilityResponse danielViewsKemasHome() {
        return view.view(new ViewRatingsAndReliabilityRequest(DANIEL_ID, "CUSTOMER", KEMAS_HOME_ID, "PROVIDER"));
    }

    @Test
    @Tag("RTG-1")
    @Tag("RTG-2")
    @Tag("RTG-3")
    @Tag("RTG-6")
    @DisplayName("Completed on Saturday 14:00, Aisyah rates on Saturday evening and Kemas Home on Monday: both show on "
            + "Monday, Kemas Home's page shows Aisyah's stars, and Daniel sees them.")
    void completedBothRateRevealAndCount() {
        onCompleted.handle(completed("reservation-event-1"));
        time.set(AISYAH_RATES_AT);
        submit.submit(new SubmitRatingRequest(AISYAH_ID, RESERVATION_ID, 4, null,
                Map.of("QUALITY_OF_WORK", 5, "PUNCTUALITY", 4, "PROFESSIONALISM", 5), List.of()));
        assertThat(danielViewsKemasHome().ratingCount()).isZero();
        assertThat(publisher.published()).isEmpty();

        time.set(KEMAS_HOME_RATES_AT);
        SubmitRatingResponse second = submit.submit(kemasHomeRates(5));

        assertThat(second.revealed()).isTrue();
        assertThat(publisher.published()).containsExactly(new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 1,
                "4.0", "5.0", "4.0", "5.0", KEMAS_HOME_RATES_AT));
        ViewRatingsAndReliabilityResponse page = danielViewsKemasHome();
        assertThat(page.ratingCount()).isEqualTo(1);
        assertThat(page.averageStars()).isEqualTo(new BigDecimal("4.0"));
        ViewRatingsAndReliabilityResponse aisyah = view.view(
                new ViewRatingsAndReliabilityRequest(KEMAS_HOME_ID, "PROVIDER", AISYAH_ID, "CUSTOMER"));
        assertThat(aisyah.averageStars()).isEqualTo(new BigDecimal("5.0"));
    }

    @Test
    @Tag("RTG-3")
    @DisplayName("Aisyah rates on Saturday, Kemas Home never does: the scheduler reveals her rating when the window "
            + "closes on Sunday 13 December, and not a minute before.")
    void theClosedWindowRevealsTheRatingAndTheScheduleIsSafeToRepeat() {
        onCompleted.handle(completed("reservation-event-1"));
        time.set(AISYAH_RATES_AT);
        submit.submit(aisyahRates(3));
        time.set(WINDOW_CLOSES_AT.minusSeconds(60));

        assertThat(reveal.revealClosed(new RevealClosedRatingWindowsRequest()).revealedReservationIds()).isEmpty();
        assertThat(danielViewsKemasHome().ratingCount()).isZero();
        time.set(WINDOW_CLOSES_AT);
        assertThat(reveal.revealClosed(new RevealClosedRatingWindowsRequest()).revealedReservationIds())
                .containsExactly(RESERVATION_ID);
        assertThat(reveal.revealClosed(new RevealClosedRatingWindowsRequest()).revealedReservationIds()).isEmpty();

        assertThat(danielViewsKemasHome().averageStars()).isEqualTo(new BigDecimal("3.0"));
        assertThat(publisher.published()).containsExactly(new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 1,
                "3.0", null, null, null, WINDOW_CLOSES_AT));
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Aisyah cancels on Friday 22:00 and pays RM45: Kemas Home sees one cancellation for her. Her free "
            + "cancellation on another reservation isn't counted.")
    void aFeePayingCancellationCountsAndAFreeOneDoesNot() {
        time.set(FRIDAY_22_00);
        onCancelled.handle(cancelled("reservation-event-3", "reservation-friday", CUSTOMER_CANCELLED_WITH_FEE));
        onCancelled.handle(cancelled("reservation-event-4", "reservation-free", CUSTOMER_CANCELLED_FREE));

        ViewRatingsAndReliabilityResponse aisyah = view.view(
                new ViewRatingsAndReliabilityRequest(KEMAS_HOME_ID, "PROVIDER", AISYAH_ID, "CUSTOMER"));

        assertThat(aisyah.cancellations()).isEqualTo(1);
        assertThat(aisyah.noRosters()).isZero();
        assertThat(aisyah.noShows()).isZero();
        assertThat(publisher.published()).containsExactly(
                new ReliabilityChangedIE("event-1", AISYAH_ID, "CUSTOMER", 1, 0, 0, 0, FRIDAY_22_00));
    }

    @Test
    @Tag("NSH-6")
    @Tag("RTG-10")
    @DisplayName("Kemas Home never came but marked the job Completed; both rated and showed. On Sunday Support confirms "
            + "the Provider No Show: the ratings are gone from Kemas Home's page and the no-show counts.")
    void aProviderNoShowRemovesTheRatingsAndCounts() {
        onCompleted.handle(completed("reservation-event-1"));
        time.set(AISYAH_RATES_AT);
        submit.submit(aisyahRates(4));
        time.set(KEMAS_HOME_RATES_AT);
        submit.submit(kemasHomeRates(5));
        assertThat(danielViewsKemasHome().ratingCount()).isEqualTo(1);
        publisher.clear();

        onCancelled.handle(cancelled("reservation-event-9", PROVIDER_NO_SHOW));

        ViewRatingsAndReliabilityResponse page = danielViewsKemasHome();
        assertThat(page.ratingCount()).isZero();
        assertThat(page.averageStars()).isNull();
        assertThat(page.noShows()).isEqualTo(1);
        assertThat(publisher.published()).hasSize(2);
        assertThat(publisher.published().get(0)).isInstanceOf(ProviderRatingRevealedIE.class);
        assertThat(publisher.published().get(1)).isInstanceOf(ReliabilityChangedIE.class);
    }

    @Test
    @Tag("RTG-11")
    @DisplayName("Support removes Aisyah's rating that includes a phone number: Kemas Home's page no longer counts it.")
    void supportRemovesARatingFromThePage() {
        onCompleted.handle(completed("reservation-event-1"));
        time.set(AISYAH_RATES_AT);
        SubmitRatingResponse aisyahs = submit.submit(aisyahRates(1));
        time.set(KEMAS_HOME_RATES_AT);
        submit.submit(kemasHomeRates(5));
        assertThat(danielViewsKemasHome().ratingCount()).isEqualTo(1);

        remove.remove(new RemoveRatingRequest(SUPPORT_ID, "SUPPORT", RESERVATION_ID, aisyahs.ratingId()));

        assertThat(danielViewsKemasHome().ratingCount()).isZero();
    }
}
