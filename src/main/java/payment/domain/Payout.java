package payment.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import kernel.Money;
import payment.domain.exception.InvalidPayoutAmountException;
import payment.domain.exception.PayoutAlreadyDeductedException;
import payment.domain.exception.PayoutNotHeldException;
import payment.domain.exception.PayoutNotPayableException;
import payment.domain.exception.PayoutReasonRequiredException;

/**
 * What one reservation pays the provider: the gross earning, less any fines deducted, is the net payout. A
 * payout to a seriously flagged business is held until Homa's founders release it or withhold it.
 */
public final class Payout {

    private final String id;
    private final String providerId;
    private final String reservationId;
    private final Money gross;
    private final PayoutKind kind;
    private final List<FineDeduction> deductions = new ArrayList<>();
    private boolean finesApplied;
    private PayoutStatus status = PayoutStatus.PAYABLE;
    private String reason;

    private Payout(String providerId, String reservationId, Money gross, PayoutKind kind) {
        this.id = new PayoutId().generate();
        this.providerId = providerId;
        this.reservationId = reservationId;
        this.gross = gross;
        this.kind = kind;
    }

    public static Payout create(String providerId, String reservationId, Money earning, PayoutKind kind) {
        if (!PaymentText.isPositive(earning)) {
            throw new InvalidPayoutAmountException(earning);
        }
        return new Payout(
                PaymentText.require(providerId, "A provider id"),
                PaymentText.require(reservationId, "A reservation id"),
                earning,
                Objects.requireNonNull(kind));
    }

    /** Holds the payout because the provider's business was seriously flagged. */
    public void hold(String why) {
        String held = requireReason(why);
        if (status != PayoutStatus.PAYABLE) {
            throw new PayoutNotPayableException(id, status);
        }
        status = PayoutStatus.HELD;
        reason = held;
    }

    /** The founders release a held payout after their review: it is paid out as normal. */
    public void releaseHold() {
        requireHeld();
        status = PayoutStatus.PAYABLE;
        reason = null;
    }

    /** The founders keep a held payout back after their review, with a reason, for example a sanctions match. */
    public void withhold(String why) {
        String kept = requireReason(why);
        requireHeld();
        status = PayoutStatus.WITHHELD;
        reason = kept;
    }

    public PayoutStatus status() {
        return status;
    }

    /** Whether the payout can be paid out now. */
    public boolean isPayable() {
        return status == PayoutStatus.PAYABLE;
    }

    /** Why the payout is held or withheld; empty while payable. */
    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    private void requireHeld() {
        if (status != PayoutStatus.HELD) {
            throw new PayoutNotHeldException(id, status);
        }
    }

    private static String requireReason(String why) {
        if (why == null || why.isBlank()) {
            throw new PayoutReasonRequiredException();
        }
        return why.strip();
    }

    void startDeducting() {
        if (finesApplied) {
            throw new PayoutAlreadyDeductedException(id);
        }
        finesApplied = true;
    }

    void deduct(String fineId, Money amount) {
        deductions.add(new FineDeduction(fineId, amount));
    }

    public Money net() {
        return gross.minus(deducted());
    }

    public Money deducted() {
        return deductions.stream().map(FineDeduction::amount).reduce(Money.ZERO, Money::plus);
    }

    public List<FineDeduction> deductions() {
        return List.copyOf(deductions);
    }

    public String id() {
        return id;
    }

    public String providerId() {
        return providerId;
    }

    public String reservationId() {
        return reservationId;
    }

    public Money gross() {
        return gross;
    }

    public PayoutKind kind() {
        return kind;
    }
}
