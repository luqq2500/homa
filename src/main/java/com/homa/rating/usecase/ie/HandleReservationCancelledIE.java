package com.homa.rating.usecase.ie;

import com.homa.ie.events.ReservationCancelledIE;
import com.homa.rating.domain.Cancellation;
import com.homa.rating.domain.CancellationCause;
import com.homa.rating.domain.CustomerId;
import com.homa.rating.domain.Party;
import com.homa.rating.domain.ProviderId;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.ReliabilityRecord;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.usecase.ProviderRatingUpdates;
import com.homa.rating.usecase.RatingEvents;
import com.homa.rating.usecase.port.in.ReservationCancelledIEHandler;
import com.homa.rating.usecase.port.out.ProcessedMessageStore;
import com.homa.rating.usecase.port.out.RatingIEPublisher;
import com.homa.rating.usecase.port.out.RatingSessionRepository;
import com.homa.rating.usecase.port.out.ReliabilityRecordRepository;

/**
 * A Reserved reservation was cancelled. By its cause it adds to the reliability of the provider or the customer
 * (RTG-10): a provider's cancellations, no-rosters and no-shows, and a customer's fee-paying cancellations and
 * no-shows; a free cancellation is never counted. Reliability changes are published for Listing. A Provider No Show
 * also voids the ratings of a job that was marked Completed, revealed or not (NSH-6), and Listing is told the new
 * numbers. A Customer No Show never had ratings (RTG-9), so only its reliability changes.
 *
 * <p>Hook for slice 5: the roster-member-left and no-show confirmation events will feed the same
 * {@link ReliabilityRecord} through handlers of their own ({@code recordMemberLeft}); a no-show counted from here
 * is never counted twice.
 */
public class HandleReservationCancelledIE implements ReservationCancelledIEHandler {

    static final String HANDLER = "rating.ReservationCancelled";

    private final RatingSessionRepository sessions;
    private final ReliabilityRecordRepository reliability;
    private final ProviderRatingUpdates providerRatings;
    private final RatingIEPublisher publisher;
    private final RatingEvents events;
    private final ProcessedMessageStore processedMessages;

    public HandleReservationCancelledIE(RatingSessionRepository sessions, ReliabilityRecordRepository reliability,
            ProviderRatingUpdates providerRatings, RatingIEPublisher publisher, RatingEvents events,
            ProcessedMessageStore processedMessages) {
        this.sessions = sessions;
        this.reliability = reliability;
        this.providerRatings = providerRatings;
        this.publisher = publisher;
        this.events = events;
        this.processedMessages = processedMessages;
    }

    @Override
    public void handle(ReservationCancelledIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        Cancellation cancellation = new Cancellation(new ReservationId(event.reservationId()),
                CancellationCause.named(event.cause()), event.customerFee(), event.providerFine(),
                event.cancelledAt());
        if (cancellation.voidsRatings()) {
            voidRatings(cancellation.reservationId());
        }
        log(Party.provider(new ProviderId(event.providerId())), cancellation);
        log(Party.customer(new CustomerId(event.customerId())), cancellation);
    }

    private void voidRatings(ReservationId reservationId) {
        sessions.findByReservationId(reservationId).ifPresent(this::voidSession);
    }

    private void voidSession(RatingSession session) {
        boolean shownBefore = session.showsRatingOfProvider();
        if (session.voidForProviderNoShow()) {
            sessions.save(session);
            providerRatings.publishWhenChanged(shownBefore, session);
        }
    }

    private void log(Party party, Cancellation cancellation) {
        ReliabilityRecord record = reliability.findByParty(party).orElseGet(() -> ReliabilityRecord.openFor(party));
        if (record.recordCancellation(cancellation)) {
            reliability.save(record);
            publisher.publish(events.reliabilityChanged(record));
        }
    }
}
