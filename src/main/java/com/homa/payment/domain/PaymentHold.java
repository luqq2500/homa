package com.homa.payment.domain;

import com.homa.kernel.Money;
import com.homa.payment.domain.exception.CheckoutNotAwaitingPaymentException;
import com.homa.payment.domain.exception.NoPaymentToCompensateException;
import com.homa.payment.domain.exception.PaymentHoldAlreadyLinkedException;
import com.homa.payment.domain.exception.PaymentNotHeldException;
import com.homa.payment.domain.exception.RefundExceedsHeldAmountException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The customer's payment for one checkout, held by Homa. It is opened PENDING when the checkout starts and may see
 * several payment attempts: a failed attempt changes nothing and the customer can retry. The first confirmed payment
 * counts and the whole total is held from then on (P1, PAY-1); any later confirmed payment for the same checkout is
 * refunded in full at once and never held (RES-12).
 *
 * <p>Every refund comes out of what is held, never more than is still held (PAY-4), and is a value returned at once
 * for the gateway to pay now (PAY-7). Payment never decides an amount: the total comes from the checkout and refunds
 * from a Settlement.
 *
 * <p>The hold is keyed by the checkout, but later events name the reservation. Once the reservation is Requested the
 * hold is linked to it, so a withdrawal, a decline, an expiry or a cancellation finds the payment to refund.
 */
public final class PaymentHold {

    private final PaymentHoldId id;
    private final CheckoutId checkoutId;
    private final CustomerId customerId;
    private final Money total;
    private final Set<PaymentAttemptId> confirmedAttempts = new HashSet<>();
    private PaymentHoldStatus status;
    private PaymentAttemptId countedAttempt;
    private Instant heldSince;
    private Money held;
    private ReservationId reservationId;

    private PaymentHold(PaymentHoldId id, CheckoutId checkoutId, CustomerId customerId, Money total) {
        this.id = Objects.requireNonNull(id, "id");
        this.checkoutId = Objects.requireNonNull(checkoutId, "checkoutId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.total = Amounts.requirePayable(total, "The checkout total");
        this.status = PaymentHoldStatus.PENDING;
        this.held = Money.zero(total.currency());
    }

    /** The checkout has started (RES-11): Payment waits for the customer's payment of the total. */
    public static PaymentHold openForCheckout(PaymentHoldId id, CheckoutId checkoutId, CustomerId customerId,
            Money total) {
        return new PaymentHold(id, checkoutId, customerId, total);
    }

    /**
     * Rebuilds a payment hold from what was stored: every attempt confirmed so far, the one that counts and when it
     * was confirmed ({@code null} until one is), what is still held, and the reservation it is linked to
     * ({@code null} until the reservation is Requested). Only the shape is checked: a counted attempt and its time
     * exactly once the payment is no longer pending, the counted attempt among the confirmed ones and none confirmed
     * without it, the amount held fitting the status, never more than the total, and a reservation only once a
     * payment is confirmed.
     */
    public static PaymentHold restore(PaymentHoldId id, CheckoutId checkoutId, CustomerId customerId, Money total,
            PaymentHoldStatus status, Set<PaymentAttemptId> confirmedAttempts, PaymentAttemptId countedAttempt,
            Instant heldSince, Money held, ReservationId reservationId) {
        PaymentHold hold = new PaymentHold(id, checkoutId, customerId, total);
        hold.status = Objects.requireNonNull(status, "status");
        hold.confirmedAttempts.addAll(confirmedAttempts);
        hold.countedAttempt = countedAttempt;
        hold.heldSince = heldSince;
        hold.held = Objects.requireNonNull(held, "held");
        hold.reservationId = reservationId;
        hold.requireStoredShape();
        return hold;
    }

    private void requireStoredShape() {
        requireShape(countedAttempt().isPresent() != (status == PaymentHoldStatus.PENDING),
                "a counted attempt exactly once its payment is confirmed");
        requireShape(countedAttempt().isPresent() == heldSince().isPresent(),
                "the time its counted attempt was confirmed");
        requireShape(countedAttempt().map(confirmedAttempts::contains).orElse(confirmedAttempts.isEmpty()),
                "its counted attempt among the confirmed ones, and no confirmed attempt without one");
        requireShape(heldFitsStatus(), "an amount held only while " + status + " allows, never more than the total");
        requireShape(reservationId == null || status != PaymentHoldStatus.PENDING,
                "a reservation only once its payment is confirmed");
    }

    /** Something, up to the total, is held while HELD; nothing before a payment is confirmed or once refunded. */
    private boolean heldFitsStatus() {
        return status == PaymentHoldStatus.HELD ? held.isPositive() && !held.isGreaterThan(total) : held.isZero();
    }

    private static void requireShape(boolean fits, String what) {
        if (!fits) {
            throw new IllegalArgumentException("A stored payment hold needs " + what);
        }
    }

    /**
     * The customer starts another attempt to pay the checkout total: a card payment, a retry after a failed attempt,
     * an online banking payment. The new attempt's id is the gateway's reference for the payment. Only while no
     * payment is confirmed: once one is held, another payment would only be refunded in full (RES-12).
     */
    public PaymentAttemptId startAttempt() {
        if (status != PaymentHoldStatus.PENDING) {
            throw new CheckoutNotAwaitingPaymentException(id, status);
        }
        return PaymentAttemptId.generate();
    }

    /**
     * The gateway confirmed one of the checkout's payment attempts, each made for the checkout total. The first
     * confirmed payment counts and is held (PAY-1); a later one is refunded in full at once and never held (RES-12).
     * The same attempt confirmed again changes nothing.
     */
    public PaymentConfirmation recordConfirmation(PaymentAttemptId attemptId, Instant confirmedAt) {
        Objects.requireNonNull(attemptId, "attemptId");
        Objects.requireNonNull(confirmedAt, "confirmedAt");
        if (!confirmedAttempts.add(attemptId)) {
            return new PaymentConfirmation.AlreadyRecorded(attemptId);
        }
        if (countedAttempt != null) {
            return new PaymentConfirmation.RefundedInFull(new Refund(id, customerId, attemptId, total));
        }
        countedAttempt = attemptId;
        status = PaymentHoldStatus.HELD;
        heldSince = confirmedAt;
        held = total;
        return new PaymentConfirmation.Counted(attemptId, total, confirmedAt);
    }

    /**
     * The reservation is Requested from this checkout's payment (RES-7): the hold now belongs to it, so later events
     * about the reservation find it. Linking the same reservation again changes nothing; a hold belongs to one
     * reservation only, and only a payment that is held can be linked.
     */
    public void linkToReservation(ReservationId requestedReservation) {
        Objects.requireNonNull(requestedReservation, "requestedReservation");
        if (requestedReservation.equals(reservationId)) {
            return;
        }
        if (reservationId != null) {
            throw new PaymentHoldAlreadyLinkedException(id, reservationId);
        }
        requireHeld();
        reservationId = requestedReservation;
    }

    /** Returns part or all of what is held to the customer, at once (PAY-7); never more than is still held (PAY-4). */
    public Refund refund(Money amount) {
        Amounts.requirePayable(amount, "A refund");
        requireHeld();
        if (amount.isGreaterThan(held)) {
            throw new RefundExceedsHeldAmountException(amount, held);
        }
        held = held.minus(amount);
        if (held.isZero()) {
            status = PaymentHoldStatus.REFUNDED;
        }
        return new Refund(id, customerId, countedAttempt, amount);
    }

    /**
     * The confirmed payment could not become a reservation: another customer's payment took the time first, or a
     * booking rule no longer passes. Everything held is refunded in full at once (RES-11).
     */
    public Refund refundRejectedCheckout() {
        return refundInFull();
    }

    /**
     * Everything still held goes back to the customer at once (PAY-7): a Requested reservation the customer withdrew
     * (REQ-2), the provider declined (REQ-3) or nobody accepted in time (REQ-4).
     */
    public Refund refundInFull() {
        requireHeld();
        return refund(held);
    }

    /**
     * Homa pays the customer compensation on top of a full refund (PAY-4). It comes from Homa, not from what is held,
     * so the amount held does not change; it goes back to the payment that counted, so only once a payment is
     * confirmed.
     */
    public Compensation compensate(Money amount) {
        Amounts.requirePayable(amount, "A compensation");
        if (countedAttempt == null) {
            throw new NoPaymentToCompensateException(id);
        }
        return new Compensation(id, customerId, countedAttempt, amount);
    }

    private void requireHeld() {
        if (status != PaymentHoldStatus.HELD) {
            throw new PaymentNotHeldException(id, status);
        }
    }

    public PaymentHoldId id() {
        return id;
    }

    public CheckoutId checkoutId() {
        return checkoutId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    /** What the customer pays for the checkout, on each attempt. */
    public Money total() {
        return total;
    }

    /** What Homa still holds for the customer: nothing until a payment is confirmed, less every refund after. */
    public Money held() {
        return held;
    }

    public PaymentHoldStatus status() {
        return status;
    }

    /**
     * Every attempt the gateway has confirmed for the checkout, the one that counts and any refunded in full
     * (RES-12), so a repeated confirmation changes nothing.
     */
    public Set<PaymentAttemptId> confirmedAttempts() {
        return Set.copyOf(confirmedAttempts);
    }

    /** The payment attempt that counts: the first one confirmed (RES-12). */
    public Optional<PaymentAttemptId> countedAttempt() {
        return Optional.ofNullable(countedAttempt);
    }

    /** When the payment that counts was confirmed, once one has been. */
    public Optional<Instant> heldSince() {
        return Optional.ofNullable(heldSince);
    }

    /** The reservation this payment is held for, once it is Requested (RES-7). */
    public Optional<ReservationId> reservationId() {
        return Optional.ofNullable(reservationId);
    }
}
