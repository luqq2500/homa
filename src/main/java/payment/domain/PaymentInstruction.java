package payment.domain;

import java.time.Instant;
import java.util.Objects;
import kernel.Money;

/** One movement of money for Stripe to carry out, not before {@code notBefore}. */
public record PaymentInstruction(Kind kind, Money amount, Instant notBefore) {

    public enum Kind {
        /** Card: let the authorisation go; nothing was charged. */
        RELEASE_AUTHORISATION,
        /** FPX: collect an increase before an edit is sent. */
        COLLECT_FROM_CUSTOMER,
        /** Return money to the customer's original payment method. */
        REFUND_CUSTOMER,
        /** Release the provider's earning, paid out per reservation. */
        RELEASE_TO_PROVIDER,
        /** Release Homa's service fee. */
        RELEASE_SERVICE_FEE
    }

    public PaymentInstruction {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(notBefore);
    }
}
