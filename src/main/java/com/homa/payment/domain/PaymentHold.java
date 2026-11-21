package com.homa.payment.domain;

import com.homa.kernel.Money;
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
        requireHeld();
        return refund(held);
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

    /** The payment attempt that counts: the first one confirmed (RES-12). */
    public Optional<PaymentAttemptId> countedAttempt() {
        return Optional.ofNullable(countedAttempt);
    }

    /** When the payment that counts was confirmed, once one has been. */
    public Optional<Instant> heldSince() {
        return Optional.ofNullable(heldSince);
    }
}
