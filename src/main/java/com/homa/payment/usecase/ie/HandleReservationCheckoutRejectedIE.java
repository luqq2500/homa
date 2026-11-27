package com.homa.payment.usecase.ie;

import com.homa.ie.events.ReservationCheckoutRejectedIE;
import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.Refund;
import com.homa.payment.domain.exception.PaymentRuleViolationException;
import com.homa.payment.usecase.IdempotencyKeys;
import com.homa.payment.usecase.exception.PaymentHoldNotFoundException;
import com.homa.payment.usecase.port.in.ReservationCheckoutRejectedIEHandler;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;

/**
 * A confirmed payment could not become a reservation: another customer's payment took the time first, or a booking
 * rule no longer passes. Everything held is refunded in full and paid through the gateway at once (P1, RES-11,
 * PAY-7). A hold that holds nothing refunds nothing.
 */
public class HandleReservationCheckoutRejectedIE implements ReservationCheckoutRejectedIEHandler {

    static final String HANDLER = "payment.ReservationCheckoutRejected";

    private final PaymentHoldRepository holds;
    private final PaymentGateway gateway;
    private final ProcessedMessageStore processedMessages;

    public HandleReservationCheckoutRejectedIE(PaymentHoldRepository holds, PaymentGateway gateway,
            ProcessedMessageStore processedMessages) {
        this.holds = holds;
        this.gateway = gateway;
        this.processedMessages = processedMessages;
    }

    @Override
    public void handle(ReservationCheckoutRejectedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        CheckoutId checkoutId = new CheckoutId(event.checkoutId());
        PaymentHold hold = holds.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new PaymentHoldNotFoundException(checkoutId.value()));
        Refund refund;
        try {
            refund = hold.refundRejectedCheckout();
        } catch (PaymentRuleViolationException nothingHeld) {
            return;
        }
        holds.save(hold);
        gateway.refund(new PaymentGateway.RefundRequest(IdempotencyKeys.rejectedCheckoutRefund(checkoutId),
                refund.attemptId().value(), refund.amount()));
    }
}
