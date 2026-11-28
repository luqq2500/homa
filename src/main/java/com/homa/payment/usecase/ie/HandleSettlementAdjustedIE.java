package com.homa.payment.usecase.ie;

import com.homa.ie.events.SettlementAdjustedIE;
import com.homa.payment.domain.PaymentHold;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.Refund;
import com.homa.payment.domain.ReservationId;
import com.homa.payment.usecase.CustomerRefunds;
import com.homa.payment.usecase.IdempotencyKeys;
import com.homa.payment.usecase.RefundReason;
import com.homa.payment.usecase.exception.AdjustedTotalMismatchException;
import com.homa.payment.usecase.exception.PayoutNotFoundException;
import com.homa.payment.usecase.exception.ReservationHoldNotFoundException;
import com.homa.payment.usecase.exception.UnknownAdjustmentReasonException;
import com.homa.payment.usecase.port.in.SettlementAdjustedIEHandler;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.LedgerRepository;
import com.homa.payment.usecase.port.out.PaymentGateway;
import com.homa.payment.usecase.port.out.PaymentHoldRepository;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.PayoutRepository;
import com.homa.payment.usecase.port.out.ProcessedMessageStore;
import com.homa.payment.usecase.port.out.TimeSource;

/**
 * A reservation's price came down after it was Reserved, without a cancellation: Aisyah accepts Siti alone and
 * Reservation works out that RM90 goes back and the reservation now costs RM110 (ROS-8). Payment executes the line and
 * never re-derives it (plan decision 1):
 * <ol>
 * <li>{@code customerRefund} goes back to the customer from the held payment at once (PAY-7), through the same
 * {@link CustomerRefunds} the other refunds use, keyed by the adjustment's id, so a replayed money call is paid
 * once. The ledger is posted and {@code RefundPaidIE} published there;</li>
 * <li>the provider's payout is lowered by the same amount, never below zero, so it is released later on what the
 * reservation now costs (PAY-3).</li>
 * </ol>
 * Before any money moves, the payout's new gross must equal {@code newTotal}: when it doesn't, Payment's payout and
 * Reservation's price disagree, and the adjustment is refused. Every failure that would leave a refund unpaid (no
 * payment linked to the reservation, no payout, a payout that is no longer waiting, more to refund than is held, an
 * amount that isn't a payable line, a reason Payment doesn't know) is refused and reported, never skipped, so the
 * relay retries it. A second adjustment of the same reservation has its own adjustment id, and so its own refund.
 */
public class HandleSettlementAdjustedIE implements SettlementAdjustedIEHandler {

    static final String HANDLER = "payment.SettlementAdjusted";

    private final PaymentHoldRepository holds;
    private final PayoutRepository payouts;
    private final ProcessedMessageStore processedMessages;
    private final CustomerRefunds customerRefunds;

    public HandleSettlementAdjustedIE(PaymentHoldRepository holds, PayoutRepository payouts, PaymentGateway gateway,
            LedgerRepository ledger, PaymentIEPublisher publisher, ProcessedMessageStore processedMessages,
            TimeSource time, EventIds eventIds) {
        this.holds = holds;
        this.payouts = payouts;
        this.processedMessages = processedMessages;
        this.customerRefunds = new CustomerRefunds(gateway, ledger, publisher, time, eventIds);
    }

    @Override
    public void handle(SettlementAdjustedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        RefundReason reason = reasonOf(event.reason());
        ReservationId reservationId = new ReservationId(event.reservationId());
        PaymentHold hold = holds.findByReservationId(reservationId)
                .orElseThrow(() -> new ReservationHoldNotFoundException(reservationId.value()));
        Payout payout = payouts.findByReservationId(reservationId)
                .orElseThrow(() -> new PayoutNotFoundException(reservationId.value()));
        Refund refund = hold.refund(event.customerRefund());
        payout.reduceByRefund(event.customerRefund());
        if (!payout.gross().equals(event.newTotal())) {
            throw new AdjustedTotalMismatchException(reservationId.value(), event.adjustmentId(), event.newTotal(),
                    payout.gross());
        }
        holds.save(hold);
        payouts.save(payout);
        customerRefunds.refund(refund, reservationId, reason, IdempotencyKeys.adjustmentRefund(event.adjustmentId()));
    }

    /** Only the reasons Payment can tell a refund by are accepted: later slices add theirs here (RPT-9, SUS-3, RPT-5). */
    private static RefundReason reasonOf(String adjustmentReason) {
        if (!RefundReason.SMALLER_ROSTER.name().equals(adjustmentReason)) {
            throw new UnknownAdjustmentReasonException(adjustmentReason);
        }
        return RefundReason.SMALLER_ROSTER;
    }
}
