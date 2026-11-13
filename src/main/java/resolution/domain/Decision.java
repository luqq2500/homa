package resolution.domain;

import java.util.Optional;
import kernel.Money;

/** The outcome the admins choose: release, a partial refund naming what the provider receives, or a full refund. */
public record Decision(Outcome outcome, Optional<Money> providerReceives) {

    public enum Outcome {
        RELEASE,
        PARTIAL_REFUND,
        FULL_REFUND
    }

    public static Decision release() {
        return new Decision(Outcome.RELEASE, Optional.empty());
    }

    public static Decision partialRefund(Money providerReceives) {
        return new Decision(Outcome.PARTIAL_REFUND, Optional.of(providerReceives));
    }

    public static Decision fullRefund() {
        return new Decision(Outcome.FULL_REFUND, Optional.empty());
    }
}
