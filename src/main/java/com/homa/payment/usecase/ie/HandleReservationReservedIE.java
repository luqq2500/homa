package com.homa.payment.usecase.ie;

import com.homa.ie.events.ReservationReservedIE;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutId;
import com.homa.payment.domain.ProviderId;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.domain.exception.PaymentRuleViolationException;
import com.homa.payment.usecase.port.in.ReservationReservedIEHandler;
import com.homa.payment.usecase.port.out.PayoutRepository;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;

/**
 * The reservation is Reserved: the provider's JOB payout opens for the reservation total, so a report made before
 * the job is marked Completed already holds it (PAY-2, RPT-6). It keeps the reservation's {@code requestedAt}, so the
 * platform fee in force then applies (P3), and its Protection Window end. A total Payment cannot pay opens nothing.
 */
public class HandleReservationReservedIE implements ReservationReservedIEHandler {

    static final String HANDLER = "payment.ReservationReserved";

    private final PayoutRepository payouts;
    private final ProcessedMessageStore processedMessages;

    public HandleReservationReservedIE(PayoutRepository payouts, ProcessedMessageStore processedMessages) {
        this.payouts = payouts;
        this.processedMessages = processedMessages;
    }

    @Override
    public void handle(ReservationReservedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        Payout payout;
        try {
            payout = Payout.openForReservation(PayoutId.generate(), new ReservationId(event.reservationId()),
                    new ProviderId(event.providerId()), event.total(), event.requestedAt(),
                    event.protectionWindowEnd());
        } catch (PaymentRuleViolationException refused) {
            return;
        }
        payouts.save(payout);
    }
}
