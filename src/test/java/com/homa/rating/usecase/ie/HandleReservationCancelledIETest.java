package com.homa.rating.usecase.ie;

import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.AISYAH_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.CUSTOMER_CANCELLED_FREE;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.CUSTOMER_CANCELLED_WITH_FEE;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.CUSTOMER_LAST_MINUTE;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.CUSTOMER_NO_SHOW;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.CUSTOMER_SUSPENDED;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.NO_ROSTER;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.PROVIDER_CANCELLED;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.PROVIDER_LAST_MINUTE;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.PROVIDER_NO_SHOW;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.PROVIDER_SUSPENDED_BY_FLAG;
import static com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs.PROVIDER_SUSPENDED_BY_SUPPORT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.FRIDAY_22_00;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.KEMAS_HOME_RATES_AT;
import static com.homa.rating.usecase.RatingUsecaseFixtures.RESERVATION_ID;
import static com.homa.rating.usecase.RatingUsecaseFixtures.cancelled;
import static com.homa.rating.usecase.RatingUsecaseFixtures.openSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ProviderRatingRevealedIE;
import com.homa.ie.events.ReliabilityChangedIE;
import com.homa.rating.domain.Party;
import com.homa.rating.domain.Rating;
import com.homa.rating.domain.RatingId;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.RatingSessionStatus;
import com.homa.rating.domain.ReliabilityCounts;
import com.homa.rating.domain.ReliabilityRecord;
import com.homa.rating.domain.Stars;
import com.homa.rating.usecase.InMemoryProcessedMessageStore;
import com.homa.rating.usecase.InMemoryRatingSessionRepository;
import com.homa.rating.usecase.InMemoryReliabilityRecordRepository;
import com.homa.rating.usecase.ProviderRatingUpdates;
import com.homa.rating.usecase.RatingEvents;
import com.homa.rating.usecase.RatingUsecaseFixtures.CancelledAs;
import com.homa.rating.usecase.RecordingRatingIEPublisher;
import com.homa.rating.usecase.SequentialEventIds;
import com.homa.rating.usecase.FixedTimeSource;
import com.homa.rating.usecase.exception.ReliabilityRecordChangedException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleReservationCancelledIETest {

    private final InMemoryRatingSessionRepository sessions = new InMemoryRatingSessionRepository();
    private final InMemoryReliabilityRecordRepository reliability = new InMemoryReliabilityRecordRepository();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final RecordingRatingIEPublisher publisher = new RecordingRatingIEPublisher();
    private final FixedTimeSource time = new FixedTimeSource(FRIDAY_22_00);
    private final RatingEvents events = new RatingEvents(new SequentialEventIds(), time);
    private final HandleReservationCancelledIE handler = new HandleReservationCancelledIE(sessions, reliability,
            new ProviderRatingUpdates(sessions, publisher, events), publisher, events, processed);

    private ReliabilityChangedIE reliabilityEvent(String id, Party party, int cancellations, int noRosters,
            int noShows) {
        return new ReliabilityChangedIE(id, party.id(), party.role().name(), cancellations, noRosters, noShows, 0,
                FRIDAY_22_00);
    }

    private ReliabilityCounts countsOf(Party party) {
        return reliability.findByParty(party).orElseThrow().countsAt(FRIDAY_22_00);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Aisyah cancels on Friday 22:00 and pays the RM45 fee: her fee-paying cancellation is logged and "
            + "published; Kemas Home's record is untouched.")
    void aFeePayingCustomerCancellationIsLogged() {
        handler.handle(cancelled("reservation-event-7", CUSTOMER_CANCELLED_WITH_FEE));

        assertThat(countsOf(AISYAH)).isEqualTo(new ReliabilityCounts(1, 0, 0, 0));
        assertThat(reliability.findByParty(KEMAS_HOME)).isEmpty();
        assertThat(reliability.saves()).hasSize(1);
        assertThat(publisher.published()).containsExactly(reliabilityEvent("event-1", AISYAH, 1, 0, 0));
        assertThat(processed.recorded()).containsExactly("reservation-event-7|rating.ReservationCancelled");
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Aisyah's free cancellation isn't counted: nothing is logged, saved or published.")
    void aFreeCancellationIsNotCounted() {
        handler.handle(cancelled("reservation-event-7", CUSTOMER_CANCELLED_FREE));

        assertThat(reliability.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(processed.recorded()).hasSize(1);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A customer suspended, and a provider suspended by a monitoring flag, have their reservations "
            + "cancelled free of fine: nothing counts.")
    void suspensionsWithoutAFineAreNotCounted() {
        handler.handle(cancelled("reservation-event-7", CUSTOMER_SUSPENDED));
        handler.handle(cancelled("reservation-event-8", PROVIDER_SUSPENDED_BY_FLAG));

        assertThat(reliability.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Aisyah cancels at 9:50 and pays RM65: a last-minute customer cancellation counts.")
    void aLastMinuteCustomerCancellationIsCounted() {
        handler.handle(cancelled("reservation-event-7", CUSTOMER_LAST_MINUTE));

        assertThat(publisher.published()).containsExactly(reliabilityEvent("event-1", AISYAH, 1, 0, 0));
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Kemas Home cancels on Wednesday and is fined RM45: it counts against Kemas Home, not Aisyah.")
    void aProviderCancellationIsLoggedAgainstTheProvider() {
        handler.handle(cancelled("reservation-event-7", PROVIDER_CANCELLED));

        assertThat(countsOf(KEMAS_HOME)).isEqualTo(new ReliabilityCounts(1, 0, 0, 0));
        assertThat(reliability.findByParty(AISYAH)).isEmpty();
        assertThat(publisher.published()).containsExactly(reliabilityEvent("event-1", KEMAS_HOME, 1, 0, 0));
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Kemas Home's van breaks down at 9:50 (last minute), it has no complete roster at 9:45, and it "
            + "doesn't come at all: a cancellation, a no-roster and a no-show, each on its own.")
    void everyProviderCauseIsCountedOnItsOwn() {
        handler.handle(cancelled("e-1", "reservation-1", PROVIDER_LAST_MINUTE));
        handler.handle(cancelled("e-2", "reservation-2", NO_ROSTER));
        handler.handle(cancelled("e-3", "reservation-3", PROVIDER_NO_SHOW));
        handler.handle(cancelled("e-4", "reservation-4", PROVIDER_SUSPENDED_BY_SUPPORT));

        assertThat(countsOf(KEMAS_HOME)).isEqualTo(new ReliabilityCounts(2, 1, 1, 0));
        assertThat(publisher.published()).containsExactly(
                reliabilityEvent("event-1", KEMAS_HOME, 1, 0, 0),
                reliabilityEvent("event-2", KEMAS_HOME, 1, 1, 0),
                reliabilityEvent("event-3", KEMAS_HOME, 1, 1, 1),
                reliabilityEvent("event-4", KEMAS_HOME, 2, 1, 1));
    }

    @Test
    @Tag("RTG-9")
    @Tag("RTG-10")
    @DisplayName("A Customer No Show gets no ratings; it shows in Aisyah's reliability instead.")
    void aCustomerNoShowShowsInReliabilityAndOpensNoRatings() {
        handler.handle(cancelled("reservation-event-7", CUSTOMER_NO_SHOW));

        assertThat(countsOf(AISYAH)).isEqualTo(new ReliabilityCounts(0, 0, 1, 0));
        assertThat(sessions.all()).isEmpty();
        assertThat(publisher.published()).containsExactly(reliabilityEvent("event-1", AISYAH, 0, 0, 1));
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A redelivered Cancelled changes nothing and publishes nothing.")
    void redeliveredEventChangesNothing() {
        handler.handle(cancelled("reservation-event-7", CUSTOMER_CANCELLED_WITH_FEE));
        publisher.clear();

        handler.handle(cancelled("reservation-event-7", CUSTOMER_CANCELLED_WITH_FEE));

        assertThat(reliability.saves()).hasSize(1);
        assertThat(publisher.published()).isEmpty();
        assertThat(countsOf(AISYAH).cancellations()).isEqualTo(1);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("The same cancellation arriving under another event id is not counted twice.")
    void theSameCancellationUnderAnotherEventIdCountsOnce() {
        handler.handle(cancelled("reservation-event-7", CUSTOMER_CANCELLED_WITH_FEE));
        publisher.clear();

        handler.handle(cancelled("reservation-event-8", CUSTOMER_CANCELLED_WITH_FEE));

        assertThat(countsOf(AISYAH).cancellations()).isEqualTo(1);
        assertThat(reliability.saves()).hasSize(1);
        assertThat(publisher.published()).isEmpty();
        assertThat(processed.recorded()).hasSize(2);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("An earlier cancellation stays in the record: a second one makes 2.")
    void aSecondCancellationAddsToTheRecord() {
        ReliabilityRecord earlier = ReliabilityRecord.openFor(AISYAH);
        reliability.given(earlier);
        handler.handle(cancelled("e-1", "reservation-1", CUSTOMER_CANCELLED_WITH_FEE));

        handler.handle(cancelled("e-2", "reservation-2", CUSTOMER_CANCELLED_WITH_FEE));

        assertThat(countsOf(AISYAH).cancellations()).isEqualTo(2);
        assertThat(reliability.saves()).hasSize(2);
        assertThat(reliability.saves().get(0)).isSameAs(earlier);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("When someone else saved Aisyah's record first the event isn't settled: it is retried and sees "
            + "the other change.")
    void aConflictingSaveLeavesTheEventToBeRetried() {
        reliability.conflictOnNextSave(AISYAH);

        assertThatThrownBy(() -> handler.handle(cancelled("reservation-event-7", CUSTOMER_CANCELLED_WITH_FEE)))
                .isInstanceOf(ReliabilityRecordChangedException.class).hasMessageContaining(AISYAH_ID);

        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A cause the Cancelled event can't carry is a broken contract and is refused loudly.")
    void anUnknownCauseIsRefused() {
        CancelledAs unknown = new CancelledAs("WITHDRAWN", CUSTOMER_CANCELLED_FREE.customerFee(),
                CUSTOMER_CANCELLED_FREE.providerFine());

        assertThatThrownBy(() -> handler.handle(cancelled("reservation-event-7", unknown)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("Kemas Home never came but marked the job Completed with an old photo and both rated. Support "
            + "confirms the Provider No Show: both ratings are removed, Listing is told Kemas Home has none left, and "
            + "the no-show counts against Kemas Home.")
    void aProviderNoShowAfterCompletedVoidsBothRevealedRatings() {
        RatingSession session = openSession();
        session.submit(Rating.by(RatingId.generate(), AISYAH, new Stars(4), null, Map.of(), List.of(),
                AISYAH_RATES_AT));
        session.submit(Rating.by(RatingId.generate(), KEMAS_HOME, new Stars(5), null, Map.of(), List.of(),
                KEMAS_HOME_RATES_AT));
        sessions.given(session);

        handler.handle(cancelled("reservation-event-9", PROVIDER_NO_SHOW));

        assertThat(session.status()).isEqualTo(RatingSessionStatus.VOID);
        assertThat(session.ratings()).isEmpty();
        assertThat(sessions.saves()).containsExactly(session);
        assertThat(publisher.published()).containsExactly(
                new ProviderRatingRevealedIE("event-1", KEMAS_HOME_ID, 0, null, null, null, null, FRIDAY_22_00),
                reliabilityEvent("event-2", KEMAS_HOME, 0, 0, 1));
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("A Provider No Show voids ratings that were still hidden too, with nothing shown to take back.")
    void aProviderNoShowVoidsHiddenRatingsWithoutPublishingProviderRatings() {
        RatingSession session = openSession();
        session.submit(Rating.by(RatingId.generate(), AISYAH, new Stars(4), null, Map.of(), List.of(),
                AISYAH_RATES_AT));
        sessions.given(session);

        handler.handle(cancelled("reservation-event-9", PROVIDER_NO_SHOW));

        assertThat(session.status()).isEqualTo(RatingSessionStatus.VOID);
        assertThat(publisher.published()).containsExactly(reliabilityEvent("event-1", KEMAS_HOME, 0, 0, 1));
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("A session already void isn't voided again: nothing more is saved.")
    void anAlreadyVoidSessionIsLeftAlone() {
        RatingSession session = openSession();
        session.voidForProviderNoShow();
        sessions.given(session);

        handler.handle(cancelled("reservation-event-9", PROVIDER_NO_SHOW));

        assertThat(sessions.saves()).isEmpty();
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("Only a Provider No Show voids ratings: another cancellation leaves a session alone.")
    void otherCausesLeaveTheRatingsAlone() {
        RatingSession session = openSession();
        sessions.given(session);

        handler.handle(cancelled("reservation-event-9", CUSTOMER_NO_SHOW));
        handler.handle(cancelled("reservation-event-10", PROVIDER_CANCELLED));

        assertThat(session.status()).isEqualTo(RatingSessionStatus.OPEN);
        assertThat(sessions.saves()).isEmpty();
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("A Provider No Show for a reservation that was never marked Completed has no ratings to void.")
    void aProviderNoShowWithoutRatingsOnlyCounts() {
        handler.handle(cancelled("reservation-event-9", PROVIDER_NO_SHOW));

        assertThat(sessions.saves()).isEmpty();
        assertThat(countsOf(KEMAS_HOME).noShows()).isEqualTo(1);
        assertThat(RESERVATION_ID).isNotBlank();
    }
}
