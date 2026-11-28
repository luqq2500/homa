package com.homa.payment.usecase.ie;

import com.homa.ie.events.ReservationExpiredIE;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.CustomerRefunds;
import com.homa.payment.usecase.IdempotencyKeys;
import com.homa.payment.usecase.RefundReason;
import com.homa.payment.usecase.port.in.ReservationExpiredIEHandler;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.LedgerRepository;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;
import com.homa.payment.usecase.port.out.TimeSource;

/**
 * A Requested reservation passed its acceptance deadline (REQ-4): everything held is refunded in full through the
 * gateway at once (PAY-7), and {@code RefundPaidIE} tells the customer it was sent. No payout exists yet, because an
 * expiry only happens while Requested.
 */
public class HandleReservationExpiredIE implements ReservationExpiredIEHandler {

    static final String HANDLER = "payment.ReservationExpired";

    private final RefundOfEndedRequest endedRequests;
    private final ProcessedMessageStore processedMessages;

    public HandleReservationExpiredIE(PaymentHoldRepository holds, PaymentGateway gateway, LedgerRepository ledger,
            PaymentIEPublisher publisher, ProcessedMessageStore processedMessages, TimeSource time,
            EventIds eventIds) {
        this.endedRequests = new RefundOfEndedRequest(holds,
                new CustomerRefunds(gateway, ledger, publisher, time, eventIds));
        this.processedMessages = processedMessages;
    }

    @Override
    public void handle(ReservationExpiredIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ReservationId reservationId = new ReservationId(event.reservationId());
        endedRequests.refundInFull(reservationId, RefundReason.EXPIRED, IdempotencyKeys.expiryRefund(reservationId));
    }
}
