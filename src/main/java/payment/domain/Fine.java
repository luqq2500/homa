package payment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import kernel.Money;
import payment.domain.exception.FineAlreadyClearedException;
import payment.domain.exception.FineAlreadyWaivedException;
import payment.domain.exception.InvalidFineAmountException;
import payment.domain.exception.WaiverReasonRequiredException;

/**
 * A fine a provider owes Homa. It is deducted from later payouts until it is cleared, unless Homa waives
 * it after review.
 */
public final class Fine {

    private final String id;
    private final String providerId;
    private final String reservationId;
    private final Money amount;
    private final FineReason reason;
    private final Instant chargedAt;
    private Money deducted = Money.ZERO;
    private String waiverReason;
    private Instant waivedAt;

    private Fine(String providerId, FineCharge charge, Instant chargedAt) {
        this.id = new FineId().generate();
        this.providerId = providerId;
        this.reservationId = charge.reservationId();
        this.amount = charge.amount();
        this.reason = charge.reason();
        this.chargedAt = chargedAt;
    }

    public static Fine charge(String providerId, FineCharge charge, Instant now) {
        if (!PaymentText.isPositive(charge.amount())) {
            throw new InvalidFineAmountException(charge.amount());
        }
        return new Fine(PaymentText.require(providerId, "A provider id"), charge, Objects.requireNonNull(now));
    }

    /** Homa waives what is left of the fine, for example for a genuine emergency. */
    public void waive(String why, Instant now) {
        if (isWaived()) {
            throw new FineAlreadyWaivedException(id);
        }
        if (outstanding().isZero()) {
            throw new FineAlreadyClearedException(id);
        }
        if (why == null || why.isBlank()) {
            throw new WaiverReasonRequiredException();
        }
        waiverReason = why.strip();
        waivedAt = Objects.requireNonNull(now);
    }

    /** What is still owed: nothing once waived or fully deducted. */
    public Money outstanding() {
        return isWaived() ? Money.ZERO : amount.minus(deducted);
    }

    /** Takes {@code part} off the fine, from a payout. */
    void deduct(Money part) {
        deducted = deducted.plus(part);
    }

    public boolean isWaived() {
        return waivedAt != null;
    }

    public boolean isCleared() {
        return outstanding().isZero();
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

    public Money amount() {
        return amount;
    }

    public FineReason reason() {
        return reason;
    }

    public Instant chargedAt() {
        return chargedAt;
    }

    public Money deducted() {
        return deducted;
    }

    public Optional<String> waiverReason() {
        return Optional.ofNullable(waiverReason);
    }

    public Optional<Instant> waivedAt() {
        return Optional.ofNullable(waivedAt);
    }
}
