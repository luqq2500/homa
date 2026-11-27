package com.homa.payment.usecase;

import com.homa.ie.events.PaymentFailedIE;
import com.homa.ie.events.PaymentHeldIE;
import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.PaymentAttemptId;
import com.homa.payment.domain.PaymentConfirmation;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.Refund;
import com.homa.payment.usecase.dto.ConfirmGatewayPaymentRequest;
import com.homa.payment.usecase.dto.ConfirmGatewayPaymentResponse;
import com.homa.payment.usecase.exception.PaymentHoldNotFoundException;
import com.homa.payment.usecase.port.in.GatewayPaymentConfirmer;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;
import com.homa.payment.usecase.port.out.TimeSource;

/**
 * The gateway's webhook about one payment attempt (EXT-2). A notification the gateway sent before is de-duplicated by
 * the gateway's event id and does nothing. A refused attempt publishes {@link PaymentFailedIE} and leaves the hold as
 * it was, so the customer can retry. A confirmed attempt is recorded on the hold: the first one counts and publishes
 * {@link PaymentHeldIE} (PAY-1); a later one for the same checkout is refunded in full through the gateway at once
 * and never published (RES-12, PAY-7); the same attempt confirmed again does nothing.
 */
public class ConfirmGatewayPayment implements GatewayPaymentConfirmer {

    static final String HANDLER = "payment.GatewayPaymentWebhook";

    private final PaymentHoldRepository holds;
    private final PaymentGateway gateway;
    private final PaymentIEPublisher publisher;
    private final ProcessedMessageStore processedMessages;
    private final TimeSource time;
    private final EventIds eventIds;

    public ConfirmGatewayPayment(PaymentHoldRepository holds, PaymentGateway gateway, PaymentIEPublisher publisher,
            ProcessedMessageStore processedMessages, TimeSource time, EventIds eventIds) {
        this.holds = holds;
        this.gateway = gateway;
        this.publisher = publisher;
        this.processedMessages = processedMessages;
        this.time = time;
        this.eventIds = eventIds;
    }

    @Override
    public ConfirmGatewayPaymentResponse confirm(ConfirmGatewayPaymentRequest request) {
        if (!processedMessages.recordFirstHandling(request.gatewayEventId(), HANDLER)) {
            return new ConfirmGatewayPaymentResponse("DUPLICATE");
        }
        CheckoutId checkoutId = new CheckoutId(request.checkoutId());
        PaymentHold hold = holds.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new PaymentHoldNotFoundException(checkoutId.value()));
        if (!request.confirmed()) {
            publisher.publish(new PaymentFailedIE(eventIds.next(), checkoutId.value(), request.failureReason(),
                    time.now()));
            return new ConfirmGatewayPaymentResponse("FAILED");
        }
        PaymentConfirmation confirmation = hold.recordConfirmation(
                new PaymentAttemptId(request.paymentReference()), request.occurredAt());
        return new ConfirmGatewayPaymentResponse(apply(hold, confirmation));
    }

    private String apply(PaymentHold hold, PaymentConfirmation confirmation) {
        return switch (confirmation) {
            case PaymentConfirmation.Counted counted -> held(hold, counted);
            case PaymentConfirmation.RefundedInFull refunded -> refunded(hold, refunded.refund());
            case PaymentConfirmation.AlreadyRecorded alreadyRecorded -> "ALREADY_RECORDED";
        };
    }

    private String held(PaymentHold hold, PaymentConfirmation.Counted counted) {
        holds.save(hold);
        publisher.publish(new PaymentHeldIE(eventIds.next(), hold.checkoutId().value(), counted.amount(),
                counted.paidAt(), time.now()));
        return "HELD";
    }

    private String refunded(PaymentHold hold, Refund refund) {
        holds.save(hold);
        gateway.refund(new PaymentGateway.RefundRequest(IdempotencyKeys.duplicatePaymentRefund(refund.attemptId()),
                refund.attemptId().value(), refund.amount()));
        return "REFUNDED";
    }
}
