package com.homa.payment.usecase.ie;

import com.homa.ie.events.ReservationCancelledIE;
import com.homa.payment.domain.Compensation;
import com.homa.payment.domain.LedgerEntry;
import com.homa.payment.domain.LedgerMovement;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.ProviderFineAccount;
import com.homa.payment.domain.ProviderId;
import com.homa.payment.domain.Refund;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.CustomerRefunds;
import com.homa.payment.usecase.IdempotencyKeys;
import com.homa.payment.usecase.RefundReason;
import com.homa.payment.usecase.exception.PayoutNotFoundException;
import com.homa.payment.usecase.exception.ReservationHoldNotFoundException;
import com.homa.payment.usecase.port.in.ReservationCancelledIEHandler;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.FineAccountRepository;
import com.homa.payment.usecase.port.out.LedgerRepository;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.PayoutRepository;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;
import com.homa.payment.usecase.port.out.TimeSource;

/**
 * A Reserved reservation was cancelled, with the settlement lines Reservation worked out. Payment executes them and
 * never re-derives one (plan decision 1; PAY-4 to PAY-7), in this order:
 * <ol>
 * <li>the payout opened at Reserved is settled: voided when nothing is owed to the provider, including a job already
 * marked Completed that turned out to be a Provider No Show (NSH-6), or turned into a FEE payout carrying
 * {@code providerPayoutGross}, held until the Protection Window ends (PAY-6). The payout's own state decides, never the
 * cause;</li>
 * <li>{@code customerRefund} goes back to the customer from the held payment now (PAY-4, PAY-7), keyed by the
 * settlement and the line;</li>
 * <li>{@code compensation} is paid by Homa now (PAY-4, PAY-7), keyed the same way;</li>
 * <li>{@code providerFine} is added to the provider's fine account, to be deducted from its payouts (PAY-4,
 * PAY-5).</li>
 * </ol>
 * Each refund and compensation sent publishes {@code RefundPaidIE}. A line that is zero is skipped. A line Payment
 * cannot execute (no payment linked to the reservation, no payout, more to refund than is held) is refused and
 * reported, never skipped, because skipping would leave a customer without their refund.
 */
public class HandleReservationCancelledIE implements ReservationCancelledIEHandler {

    static final String HANDLER = "payment.ReservationCancelled";

    private final PaymentHoldRepository holds;
    private final PayoutRepository payouts;
    private final FineAccountRepository fineAccounts;
    private final LedgerRepository ledger;
    private final ProcessedMessageStore processedMessages;
    private final TimeSource time;
    private final CustomerRefunds customerRefunds;

    public HandleReservationCancelledIE(PaymentHoldRepository holds, PayoutRepository payouts,
            FineAccountRepository fineAccounts, PaymentGateway gateway, LedgerRepository ledger,
            PaymentIEPublisher publisher, ProcessedMessageStore processedMessages, TimeSource time,
            EventIds eventIds) {
        this.holds = holds;
        this.payouts = payouts;
        this.fineAccounts = fineAccounts;
        this.ledger = ledger;
        this.processedMessages = processedMessages;
        this.time = time;
        this.customerRefunds = new CustomerRefunds(gateway, ledger, publisher, time, eventIds);
    }

    @Override
    public void handle(ReservationCancelledIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        ReservationId reservationId = new ReservationId(event.reservationId());
        PaymentHold hold = holds.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationHoldNotFoundException(reservationId.value()));
        settlePayout(reservationId, event);
        refundCustomer(hold, reservationId, event);
        compensateCustomer(hold, reservationId, event);
        fineProvider(reservationId, event);
    }

    private void settlePayout(ReservationId reservationId, ReservationCancelledIE event) {
        Payout payout = payouts.findByReservationId(reservationId)
                .orElseThrow(() -> new PayoutNotFoundException(reservationId.value()));
        payout.settleCancellation(event.providerPayoutGross());
        payouts.save(payout);
    }

    private void refundCustomer(PaymentHold hold, ReservationId reservationId, ReservationCancelledIE event) {
        if (!event.customerRefund().isPositive()) {
            return;
        }
        Refund refund = hold.refund(event.customerRefund());
        holds.save(hold);
        customerRefunds.refund(refund, reservationId, RefundReason.CANCELLED,
                IdempotencyKeys.settlementRefund(event.settlementId()));
    }

    private void compensateCustomer(PaymentHold hold, ReservationId reservationId, ReservationCancelledIE event) {
        if (!event.compensation().isPositive()) {
            return;
        }
        Compensation compensation = hold.compensate(event.compensation());
        customerRefunds.compensate(compensation, reservationId,
                IdempotencyKeys.settlementCompensation(event.settlementId()));
    }

    private void fineProvider(ReservationId reservationId, ReservationCancelledIE event) {
        if (!event.providerFine().isPositive()) {
            return;
        }
        ProviderId providerId = new ProviderId(event.providerId());
        ProviderFineAccount account = fineAccounts.findByProviderId(providerId)
                .orElseGet(() -> ProviderFineAccount.openFor(providerId));
        account.recordFine(reservationId, event.providerFine(), event.cancelledAt());
        fineAccounts.save(account);
        ledger.post(new LedgerEntry(LedgerMovement.FINE_IMPOSED, event.providerFine(), time.now()));
    }
}
