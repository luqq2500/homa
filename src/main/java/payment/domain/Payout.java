package payment.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kernel.Money;
import payment.domain.exception.InvalidPayoutAmountException;
import payment.domain.exception.PayoutAlreadyDeductedException;

/** What one reservation pays the provider: the gross earning, less any fines deducted, is the net payout. */
public final class Payout {

    private final String id;
    private final String providerId;
    private final String reservationId;
    private final Money gross;
    private final PayoutKind kind;
    private final List<FineDeduction> deductions = new ArrayList<>();
    private boolean finesApplied;

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
