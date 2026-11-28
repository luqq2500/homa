package com.homa.payment.usecase.ie;

import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.Refund;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.domain.exception.PaymentRuleViolationException;
import com.homa.payment.usecase.CustomerRefunds;
import com.homa.payment.usecase.RefundReason;
import com.homa.payment.usecase.exception.ReservationHoldNotFoundException;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;

/**
 * What a Requested reservation that ended without a job costs the customer: nothing. Everything held is refunded in
 * full, at once (REQ-2, REQ-3, REQ-4, PAY-7). It is the same for a withdrawal, a decline and an expiry; only the
 * reason and the idempotency key differ. Withdrawn, Declined and Expired only happen while Requested, so no payout
 * exists yet: the JOB payout opens when the reservation is Reserved. A hold that holds nothing refunds nothing.
 */
final class RefundOfEndedRequest {

    private final PaymentHoldRepository holds;
    private final CustomerRefunds refunds;

    RefundOfEndedRequest(PaymentHoldRepository holds, CustomerRefunds refunds) {
        this.holds = holds;
        this.refunds = refunds;
    }

    void refundInFull(ReservationId reservationId, RefundReason reason, String idempotencyKey) {
        PaymentHold hold = holds.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationHoldNotFoundException(reservationId.value()));
        Refund refund;
        try {
            refund = hold.refundInFull();
        } catch (PaymentRuleViolationException nothingHeld) {
            return;
        }
        holds.save(hold);
        refunds.refund(refund, reservationId, reason, idempotencyKey);
    }
}
