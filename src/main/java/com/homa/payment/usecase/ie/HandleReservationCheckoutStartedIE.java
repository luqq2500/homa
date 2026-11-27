package com.homa.payment.usecase.ie;

import com.homa.ie.events.ReservationCheckoutStartedIE;
import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.CustomerId;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.PaymentHoldId;
import com.homa.payment.domain.exception.PaymentRuleViolationException;
import com.homa.payment.usecase.port.in.ReservationCheckoutStartedIEHandler;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;

/**
 * A customer started a checkout: Payment opens a pending hold for its total, which may see several payment attempts
 * (RES-11). Nothing is held until a payment is confirmed. A total Payment cannot take (not positive, or not in whole
 * sen) opens nothing, so the customer's payment is refused instead of the event being retried forever.
 */
public class HandleReservationCheckoutStartedIE implements ReservationCheckoutStartedIEHandler {

    static final String HANDLER = "payment.ReservationCheckoutStarted";

    private final PaymentHoldRepository holds;
    private final ProcessedMessageStore processedMessages;

    public HandleReservationCheckoutStartedIE(PaymentHoldRepository holds, ProcessedMessageStore processedMessages) {
        this.holds = holds;
        this.processedMessages = processedMessages;
    }

    @Override
    public void handle(ReservationCheckoutStartedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        PaymentHold hold;
        try {
            hold = PaymentHold.openForCheckout(PaymentHoldId.generate(), new CheckoutId(event.checkoutId()),
                    new CustomerId(event.customerId()), event.total());
        } catch (PaymentRuleViolationException refused) {
            return;
        }
        holds.save(hold);
    }
}
