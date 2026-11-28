package com.homa.payment.usecase.ie;

import com.homa.ie.events.ReservationRequestedIE;
import com.homa.payment.domain.CheckoutId;
import com.homa.payment.domain.LedgerEntry;
import com.homa.payment.domain.LedgerMovement;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.exception.PaymentHoldNotFoundException;
import com.homa.payment.usecase.port.in.ReservationRequestedIEHandler;
import com.homa.payment.usecase.port.out.LedgerRepository;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;
import com.homa.payment.usecase.port.out.TimeSource;

/**
 * The reservation is Requested (RES-7): Payment links the hold of the checkout to the reservation, because the
 * events that follow (a withdrawal, a decline, an expiry, a cancellation) name the reservation and not the checkout.
 * The payment is held from this moment (PAY-1), so it is posted to the ledger here. Events about one reservation
 * arrive in order, so Requested always comes before Withdrawn, Declined, Expired or Cancelled.
 */
public class HandleReservationRequestedIE implements ReservationRequestedIEHandler {

    static final String HANDLER = "payment.ReservationRequested";

    private final PaymentHoldRepository holds;
    private final LedgerRepository ledger;
    private final ProcessedMessageStore processedMessages;
    private final TimeSource time;

    public HandleReservationRequestedIE(PaymentHoldRepository holds, LedgerRepository ledger,
            ProcessedMessageStore processedMessages, TimeSource time) {
        this.holds = holds;
        this.ledger = ledger;
        this.processedMessages = processedMessages;
        this.time = time;
    }

    @Override
    public void handle(ReservationRequestedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        CheckoutId checkoutId = new CheckoutId(event.checkoutId());
        PaymentHold hold = holds.findByCheckoutId(checkoutId)
                .orElseThrow(() -> new PaymentHoldNotFoundException(checkoutId.value()));
        hold.linkToReservation(new ReservationId(event.reservationId()));
        holds.save(hold);
        ledger.post(new LedgerEntry(LedgerMovement.PAYMENT_HELD, hold.held(), time.now()));
    }
}
