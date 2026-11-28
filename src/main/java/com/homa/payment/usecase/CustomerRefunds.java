package com.homa.payment.usecase;

import com.homa.ie.events.RefundPaidIE;
import com.homa.payment.domain.Compensation;
import com.homa.payment.domain.LedgerEntry;
import com.homa.payment.domain.LedgerMovement;
import com.homa.payment.domain.Refund;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.LedgerRepository;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.TimeSource;
import java.time.Instant;

/**
 * The one place Payment sends money to a customer (PAY-7). A refund the domain has taken from the held payment, or a
 * compensation Homa pays on top of it, goes through the gateway at once with its idempotency key, is posted to the
 * ledger (PAY-4) and is announced with {@link RefundPaidIE}. "Paid" means sent: arrival depends on the banks (EXT-3).
 */
public final class CustomerRefunds {

    private final PaymentGateway gateway;
    private final LedgerRepository ledger;
    private final PaymentIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public CustomerRefunds(PaymentGateway gateway, LedgerRepository ledger, PaymentIEPublisher publisher,
            TimeSource time, EventIds eventIds) {
        this.gateway = gateway;
        this.ledger = ledger;
        this.publisher = publisher;
        this.time = time;
        this.eventIds = eventIds;
    }

    /** Sends the refund the held payment has just given up. */
    public void refund(Refund refund, ReservationId reservationId, RefundReason reason, String idempotencyKey) {
        gateway.refund(new PaymentGateway.RefundRequest(idempotencyKey, refund.attemptId().value(), refund.amount()));
        Instant sentAt = time.now();
        ledger.post(new LedgerEntry(LedgerMovement.REFUND_SENT, refund.amount(), sentAt));
        publisher.publish(new RefundPaidIE(eventIds.next(), reservationId.value(), refund.customerId().value(),
                refund.amount(), reason.name(), sentAt));
    }

    /** Sends the compensation Homa pays from its own money (PAY-4). */
    public void compensate(Compensation compensation, ReservationId reservationId, String idempotencyKey) {
        gateway.payCompensation(new PaymentGateway.CompensationRequest(idempotencyKey,
                compensation.attemptId().value(), compensation.amount()));
        Instant sentAt = time.now();
        ledger.post(new LedgerEntry(LedgerMovement.COMPENSATION_PAID, compensation.amount(), sentAt));
        publisher.publish(new RefundPaidIE(eventIds.next(), reservationId.value(),
                compensation.customerId().value(), compensation.amount(), RefundReason.COMPENSATION.name(), sentAt));
    }
}
