package reservation.domain;

import java.util.Optional;
import kernel.Money;
import reservation.domain.exception.InvalidDecisionAmountException;

/**
 * The founders' decision on a disputed reservation, received from {@code resolution}: release what was at
 * stake to the provider, a partial refund naming what the provider receives, or a full refund.
 */
public record CaseDecision(Outcome outcome, Optional<Money> providerReceives) {

    public enum Outcome {
        RELEASE,
        PARTIAL_REFUND,
        FULL_REFUND
    }

    public static CaseDecision release() {
        return new CaseDecision(Outcome.RELEASE, Optional.empty());
    }

    public static CaseDecision fullRefund() {
        return new CaseDecision(Outcome.FULL_REFUND, Optional.empty());
    }

    public static CaseDecision partialRefund(Money providerReceives) {
        return new CaseDecision(Outcome.PARTIAL_REFUND, Optional.of(providerReceives));
    }

    /** What the provider earns when {@code atStake} was in dispute. */
    Money providerEarning(Money atStake) {
        return switch (outcome) {
            case RELEASE -> atStake;
            case FULL_REFUND -> Money.ZERO;
            case PARTIAL_REFUND -> requireWithin(providerReceives.orElseThrow(), atStake);
        };
    }

    private static Money requireWithin(Money amount, Money atStake) {
        if (amount.isNegative() || amount.isGreaterThan(atStake)) {
            throw new InvalidDecisionAmountException(amount, atStake);
        }
        return amount;
    }
}
