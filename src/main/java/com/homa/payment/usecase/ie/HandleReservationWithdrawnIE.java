package com.homa.payment.usecase.ie;

import com.homa.ie.events.ReservationWithdrawnIE;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.CustomerRefunds;
import com.homa.payment.usecase.IdempotencyKeys;
import com.homa.payment.usecase.RefundReason;
import com.homa.payment.usecase.port.in.ReservationWithdrawnIEHandler;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.LedgerRepository;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;
import com.homa.payment.usecase.port.out.TimeSource;

/**
 * The customer withdrew a Requested reservation (REQ-2): everything held is refunded in full through the gateway at
 * once (PAY-7), and {@code RefundPaidIE} tells the customer it was sent. No payout exists yet, because a withdrawal
 * only happens while Requested.
 */
public class HandleReservationWithdrawnIE implements ReservationWithdrawnIEHandler {

    static final String HANDLER = "payment.ReservationWithdrawn";

    private final RefundOfEndedRequest endedRequests;
    private final ProcessedMessageStore processedMessages;

    public HandleReservationWithdrawnIE(PaymentHoldRepository holds, PaymentGateway gateway, LedgerRepository ledger,
            PaymentIEPublisher publisher, ProcessedMessageStore processedMessages, TimeSource time,
            EventIds eventIds) {
        this.endedRequests = new RefundOfEndedRequest(holds,
                new CustomerRefunds(gateway, ledger, publisher, time, eventIds));
        this.processedMessages = processedMessages;
    }

    @Override
    public void handle(ReservationWithdrawnIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ReservationId reservationId = new ReservationId(event.reservationId());
        endedRequests.refundInFull(reservationId, RefundReason.WITHDRAWN,
                IdempotencyKeys.withdrawalRefund(reservationId));
    }
}
