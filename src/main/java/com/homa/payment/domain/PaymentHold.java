package com.homa.payment.domain;

import com.homa.kernel.Money;
import com.homa.payment.domain.exception.PaymentNotHeldException;
import com.homa.payment.domain.exception.PaymentNotPendingException;
import com.homa.payment.domain.exception.RefundExceedsHeldAmountException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The customer's payment for one checkout, held by Homa. It is opened PENDING when the checkout starts, becomes HELD
 * when the gateway secures the payment (PAY-1), and every refund comes out of it, never more than is still held
 * (PAY-4). Payment never decides an amount: the total comes from the checkout and refunds from a Settlement.
 *
 * <p>Room is kept for later slices: a top-up (REQ-1, REQ-6) adds to what is held, and applying a Settlement
 * (PAY-4, PAY-7) refunds its refund line. Each refund will carry the settlement line's id as its idempotency key; the
 * use case layer passes it through to the gateway.
 */
public final class PaymentHold {

    private final PaymentHoldId id;
    private final CheckoutId checkoutId;
    private final CustomerId customerId;
    private final Money total;
    private PaymentHoldStatus status;
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

    /** The gateway secured the payment: the whole total is held from then on, and the provider can't receive it (PAY-1). */
    public void hold(Instant paidAt) {
        Objects.requireNonNull(paidAt, "paidAt");
        if (status != PaymentHoldStatus.PENDING) {
            throw new PaymentNotPendingException(id, status);
        }
        status = PaymentHoldStatus.HELD;
        heldSince = paidAt;
        held = total;
    }

    /** Returns part or all of what is held to the customer; never more than is still held. */
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
        return new Refund(id, customerId, amount);
    }

    /**
     * The payment landed after the checkout hold expired and the reservation could not be made: everything still
     * held is refunded at once (RES-12).
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

    /** What the customer pays for the checkout. */
    public Money total() {
        return total;
    }

    /** What Homa still holds for the customer: nothing until the payment is secured, less every refund after. */
    public Money held() {
        return held;
    }

    public PaymentHoldStatus status() {
        return status;
    }

    /** When the payment was secured, once it has been. */
    public Optional<Instant> heldSince() {
        return Optional.ofNullable(heldSince);
    }
}
