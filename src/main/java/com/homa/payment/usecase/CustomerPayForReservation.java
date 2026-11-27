package com.homa.payment.usecase;

import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.CustomerId;
import com.homa.payment.domain.PaymentAttemptId;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.usecase.dto.PayForReservationRequest;
import com.homa.payment.usecase.dto.PayForReservationResponse;
import com.homa.payment.usecase.exception.PaymentHoldNotFoundException;
import com.homa.payment.usecase.exception.PaymentNotAllowedException;
import com.homa.payment.usecase.port.in.ReservationPayer;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;

/**
 * The customer pays for the checkout they started: a new payment attempt for the checkout total opens a gateway
 * session, whose reference is the attempt's id (PAY-1). The attempt changes nothing until the gateway's webhook
 * confirms it; if it fails, the customer can try again (RES-11). Nothing is saved or published here.
 */
public class CustomerPayForReservation implements ReservationPayer {

    private final PaymentHoldRepository holds;
    private final PaymentGateway gateway;

    public CustomerPayForReservation(PaymentHoldRepository holds, PaymentGateway gateway) {
        this.holds = holds;
        this.gateway = gateway;
    }

    @Override
    public PayForReservationResponse pay(PayForReservationRequest request) {
        CheckoutId checkoutId = new CheckoutId(request.checkoutId());
        PaymentHold hold = holds.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new PaymentHoldNotFoundException(checkoutId.value()));
        if (!hold.customerId().equals(new CustomerId(request.customerId()))) {
            throw new PaymentNotAllowedException(request.customerId(), checkoutId.value());
        }
        PaymentAttemptId attemptId = hold.startAttempt();
        PaymentGateway.Session session = gateway.startPayment(new PaymentGateway.SessionRequest(
                IdempotencyKeys.paymentAttempt(attemptId), attemptId.value(), checkoutId.value(), hold.total()));
        return new PayForReservationResponse(checkoutId.value(), attemptId.value(), session.sessionId(),
                session.redirectUrl(), hold.total());
    }
}
