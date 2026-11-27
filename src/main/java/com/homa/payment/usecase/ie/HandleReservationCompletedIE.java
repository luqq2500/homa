package com.homa.payment.usecase.ie;

import com.homa.ie.events.ReservationCompletedIE;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.domain.exception.PaymentRuleViolationException;
import com.homa.payment.usecase.exception.PayoutNotFoundException;
import com.homa.payment.usecase.port.in.ReservationCompletedIEHandler;
import com.homa.payment.usecase.port.out.PayoutRepository;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;

/**
 * The provider marked the job Completed (JOB-3): the JOB payout opened at Reserved is owed, and releasable once the
 * Protection Window it already holds has ended and no report is open (PAY-2, RPT-6). The event's
 * {@code payoutGross} is informational; the payout keeps its own gross. A payout that is no longer waiting for its job
 * (already marked, paying a fee, released or void) changes nothing.
 */
public class HandleReservationCompletedIE implements ReservationCompletedIEHandler {

    static final String HANDLER = "payment.ReservationCompleted";

    private final PayoutRepository payouts;
    private final ProcessedMessageStore processedMessages;

    public HandleReservationCompletedIE(PayoutRepository payouts, ProcessedMessageStore processedMessages) {
        this.payouts = payouts;
        this.processedMessages = processedMessages;
    }

    @Override
    public void handle(ReservationCompletedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        Payout payout = payouts.findByReservationId(new ReservationId(event.reservationId()))
                .orElseThrow(() -> new PayoutNotFoundException(event.reservationId()));
        try {
            payout.markCompleted(event.completedAt());
        } catch (PaymentRuleViolationException notAwaitingCompletion) {
            return;
        }
        payouts.save(payout);
    }
}
