package com.homa.rating.usecase.ie;

import com.homa.ie.events.ReservationCompletedIE;
import com.homa.rating.domain.CustomerId;
import com.homa.rating.domain.ProviderId;
import com.homa.rating.domain.RatingSession;
import com.homa.rating.domain.ReservationId;
import com.homa.rating.domain.exception.RatingWindowClosedException;
import com.homa.rating.usecase.port.in.ReservationCompletedIEHandler;
import com.homa.rating.usecase.port.out.ProcessedMessageStore;
import com.homa.rating.usecase.port.out.RatingSessionRepository;

/**
 * The provider marked the job Completed (JOB-3): ratings open for the reservation, until 7 days after its Protection
 * Window ends, by the window in force when it was Requested (RTG-2, P3). Only a Completed reservation gets ratings
 * (RTG-9). A job marked Completed after the window would have closed gets none, and the event is settled without
 * retrying. A reservation that already has its session is left alone.
 */
public class HandleReservationCompletedIE implements ReservationCompletedIEHandler {

    static final String HANDLER = "rating.ReservationCompleted";

    private final RatingSessionRepository sessions;
    private final ProcessedMessageStore processedMessages;

    public HandleReservationCompletedIE(RatingSessionRepository sessions, ProcessedMessageStore processedMessages) {
        this.sessions = sessions;
        this.processedMessages = processedMessages;
    }

    @Override
    public void handle(ReservationCompletedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ReservationId reservationId = new ReservationId(event.reservationId());
        if (sessions.findByReservationId(reservationId).isPresent()) {
            return;
        }
        try {
            sessions.save(RatingSession.open(reservationId, new CustomerId(event.customerId()),
                    new ProviderId(event.providerId()), event.completedAt(), event.protectionWindowEnd(),
                    event.requestedAt()));
        } catch (RatingWindowClosedException windowAlreadyClosed) {
            return;
        }
    }
}
