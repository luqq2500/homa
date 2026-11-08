package payment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kernel.Money;
import payment.domain.PaymentInstruction.Kind;
import payment.domain.exception.InvalidPaymentAmountException;
import payment.domain.exception.InvalidPaymentStateException;
import payment.domain.exception.PaymentAlreadyAcceptedException;
import payment.domain.exception.PaymentAlreadySettledException;
import payment.domain.exception.PaymentNotAcceptedException;
import payment.domain.exception.SettlementForAnotherReservationException;
import payment.domain.exception.SettlementMismatchException;

/**
 * The customer's payment for one reservation. A card is authorised at request and charged when the
 * provider accepts; FPX is paid upfront. Homa holds every charged payment until it is released or
 * refunded, and keeps a record of each part so the payment is always fully accounted for.
 */
public final class Payment {

    private final String id;
    private final String reservationId;
    private final PaymentMethod method;
    private Money amount;
    private Money collected;
    private Money released = Money.ZERO;
    private Money refunded = Money.ZERO;
    private PaymentState state;
    private boolean accepted;
    private boolean settled;

    private Payment(String reservationId, PaymentMethod method, Money amount, Money collected, PaymentState state) {
        this.id = new PaymentId().generate();
        this.reservationId = PaymentText.require(reservationId, "A reservation id");
        this.method = method;
        this.amount = requirePositive(amount);
        this.collected = collected;
        this.state = state;
    }

    /** A card is checked for the total and nothing is charged yet. */
    public static Payment authoriseCard(String reservationId, Money total, Instant now) {
        Objects.requireNonNull(now);
        return new Payment(reservationId, PaymentMethod.CARD, total, Money.ZERO, PaymentState.AUTHORISED);
    }

    /** FPX cannot be authorised first, so the total is paid upfront and held. */
    public static Payment payUpfrontByFpx(String reservationId, Money total, Instant now) {
        Objects.requireNonNull(now);
        return new Payment(reservationId, PaymentMethod.FPX, total, total, PaymentState.HELD);
    }

    /**
     * The provider accepted. A card is charged for the total; an FPX payment, already held, is now kept for
     * the booking.
     */
    public void capture(Instant now) {
        Objects.requireNonNull(now);
        requireOpenRequest("captured");
        if (method == PaymentMethod.CARD) {
            collected = amount;
            state = PaymentState.HELD;
        }
        accepted = true;
    }

    /**
     * The request was edited before acceptance. A card's authorisation moves to the new total; for FPX the
     * customer pays an increase, or is refunded a decrease.
     *
     * @return the difference, positive when the customer pays more
     */
    public Money adjustTotal(Money newTotal) {
        requireOpenRequest("adjusted");
        Money difference = requirePositive(newTotal).minus(amount);
        if (method == PaymentMethod.FPX && difference.isNegative()) {
            refunded = refunded.minus(difference);
        } else if (method == PaymentMethod.FPX) {
            collected = collected.plus(difference);
        }
        amount = newTotal;
        return difference;
    }

    /**
     * The request ended before the provider accepted: declined, expired or cancelled. It costs the customer
     * nothing: a card's authorisation is released, and an FPX payment is refunded in full.
     */
    public PaymentInstruction cancelBeforeAcceptance(Instant now) {
        requireOpenRequest("ended before acceptance");
        if (method == PaymentMethod.CARD) {
            state = PaymentState.AUTHORISATION_RELEASED;
            return new PaymentInstruction(Kind.RELEASE_AUTHORISATION, amount, now);
        }
        refunded = refunded.plus(amount);
        state = PaymentState.REFUNDED;
        return new PaymentInstruction(Kind.REFUND_CUSTOMER, amount, now);
    }

    /**
     * Carries out a settlement on an accepted booking's held payment: the refund now, and the provider's
     * earning and Homa's fee from the settlement's release time. A payment is settled once.
     */
    public List<PaymentInstruction> settle(Settlement settlement, Instant now) {
        requireSettleable(settlement);
        List<PaymentInstruction> instructions = new ArrayList<>();
        addIfAny(instructions, Kind.REFUND_CUSTOMER, settlement.customerRefund(), now);
        addIfAny(instructions, Kind.RELEASE_TO_PROVIDER, settlement.providerEarning(), settlement.releaseAt());
        addIfAny(instructions, Kind.RELEASE_SERVICE_FEE, settlement.serviceFee(), settlement.releaseAt());
        refunded = refunded.plus(settlement.customerRefund());
        released = released.plus(settlement.providerEarning()).plus(settlement.serviceFee());
        state = released.isZero() ? PaymentState.REFUNDED : PaymentState.RELEASED;
        settled = true;
        return List.copyOf(instructions);
    }

    /** Everything collected from the customer has been released or refunded. */
    public boolean isFullyAccounted() {
        return released.plus(refunded).equals(collected);
    }

    public String id() {
        return id;
    }

    public String reservationId() {
        return reservationId;
    }

    public PaymentMethod method() {
        return method;
    }

    /** The booking total this payment is for. */
    public Money amount() {
        return amount;
    }

    /** What has actually been taken from the customer. */
    public Money collected() {
        return collected;
    }

    public Money released() {
        return released;
    }

    public Money refunded() {
        return refunded;
    }

    public PaymentState state() {
        return state;
    }

    public boolean isAccepted() {
        return accepted;
    }

    /** Before acceptance, while the card is authorised or the FPX payment is held. */
    private void requireOpenRequest(String action) {
        if (accepted) {
            throw new PaymentAlreadyAcceptedException();
        }
        if (state != PaymentState.AUTHORISED && state != PaymentState.HELD) {
            throw new InvalidPaymentStateException(state, action);
        }
    }

    private void requireSettleable(Settlement settlement) {
        if (!settlement.reservationId().equals(reservationId)) {
            throw new SettlementForAnotherReservationException(reservationId, settlement.reservationId());
        }
        if (settled) {
            throw new PaymentAlreadySettledException(reservationId);
        }
        if (!accepted) {
            throw new PaymentNotAcceptedException();
        }
        if (!settlement.total().equals(amount)) {
            throw new SettlementMismatchException(settlement.total(), amount);
        }
    }

    private static void addIfAny(List<PaymentInstruction> instructions, Kind kind, Money amount, Instant notBefore) {
        if (!amount.isZero()) {
            instructions.add(new PaymentInstruction(kind, amount, notBefore));
        }
    }

    private static Money requirePositive(Money amount) {
        if (!PaymentText.isPositive(amount)) {
            throw new InvalidPaymentAmountException(amount);
        }
        return amount;
    }
}
